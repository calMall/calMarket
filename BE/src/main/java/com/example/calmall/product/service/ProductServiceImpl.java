package com.example.calmall.product.service;

import com.example.calmall.product.dto.ProductDetailResponseDto;
import com.example.calmall.product.entity.Product;
import com.example.calmall.product.repository.ProductRepository;
import com.example.calmall.product.text.DescriptionCleanerFacade;
import com.example.calmall.product.text.DescriptionHtmlToPlain;
import com.example.calmall.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 商品の取得・正規化を担当
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final RakutenApiService rakutenApiService;
    private final ReviewRepository reviewRepository;

    private final DescriptionCleanerFacade descriptionCleanerFacade;

    // 占位/ノイズとして判定する文言（caption の修復にも使う）
    private static final String[] BANNED_PHRASES = {
            "入力が必要です。原文を入力してください。",
            "please provide input",
            "no input provided",
            "placeholder",
            "これはテストです"
    };

    // <li> の先頭に装飾記号が残っているか（再整形の目安）
    private static final Pattern BULLET_DECOR = Pattern.compile("<li>\\s*[・●•\\-*]");

    @Override
    public ResponseEntity<ProductDetailResponseDto> getProductDetail(String itemCode) {
        // 1) まずDBを参照
        Product product = productRepository.findByItemCode(itemCode).orElse(null);

        if (product == null) {
            // 2) DBにない場合は楽天APIから取得
            log.info("[source=RakutenAPI] DB未登録 → 楽天API照会 itemCode={}", itemCode);
            product = rakutenApiService.fetchProductFromRakuten(itemCode).orElse(null);
            if (product == null) {
                log.warn("[not-found] 楽天APIから取得不可 itemCode={}", itemCode);
                return new ResponseEntity<>(buildFailResponse(), HttpStatus.BAD_REQUEST);
            }

            // 3) 初回保存前に必要なら LLM で整形（判定は desc のみ）
            if (needsCleanDesc(product.getDescriptionHtml(), product.getDescriptionPlain())) {
                log.debug("[normalize] 新規取得 → LLM 整形開始 itemCode={}", itemCode);
                final String cleanHtml = descriptionCleanerFacade.buildCleanHtml(
                        product.getDescriptionHtml(),
                        product.getDescriptionPlain(),
                        product.getItemCaption()
                );
                final String cleanPlain = DescriptionHtmlToPlain.toPlain(cleanHtml);
                product.setDescriptionHtml(cleanHtml);
                product.setDescriptionPlain(cleanPlain);
                // caption が空 or 占位文言なら差し替え
                product.setItemCaption(fixCaptionIfNeeded(product.getItemCaption(), cleanPlain));
            } else {
                // desc は既に十分な体裁だが、caption の占位は別途修正
                product.setItemCaption(fixCaptionIfNeeded(product.getItemCaption(), product.getDescriptionPlain()));
                log.debug("[normalize] 新規取得だが整形不要と判定 itemCode={}", itemCode);
            }

            product = productRepository.save(product);
            log.info("[persist] 楽天APIから取得した商品を保存 itemCode={}", product.getItemCode());

        } else {
            // 4) DBにある場合：必要に応じて LLM、ついでに caption も補正
            log.info("[source=DB] 既存商品を取得 itemCode={} name={}", product.getItemCode(), product.getItemName());

            boolean dirty = false;

            // desc が粗い場合のみ LLM 実行
            if (needsCleanDesc(product.getDescriptionHtml(), product.getDescriptionPlain())) {
                log.debug("[normalize] DB命中だが未整形/不正規 → LLM 整形開始 itemCode={}", product.getItemCode());
                final String cleanHtml = descriptionCleanerFacade.buildCleanHtml(
                        product.getDescriptionHtml(),
                        product.getDescriptionPlain(),
                        product.getItemCaption()
                );
                final String cleanPlain = DescriptionHtmlToPlain.toPlain(cleanHtml);

                if (!equalsSafe(cleanHtml, product.getDescriptionHtml())) {
                    product.setDescriptionHtml(cleanHtml);
                    dirty = true;
                }
                if (!equalsSafe(cleanPlain, product.getDescriptionPlain())) {
                    product.setDescriptionPlain(cleanPlain);
                    dirty = true;
                }

                // caption が空 or 占位文言なら LLM 結果で補正
                final String fixedCaption = fixCaptionIfNeeded(product.getItemCaption(), cleanPlain);
                if (!equalsSafe(fixedCaption, product.getItemCaption())) {
                    product.setItemCaption(fixedCaption);
                    dirty = true;
                }

                if (dirty) {
                    product = productRepository.save(product);
                    log.info("[normalize] 説明文をクリーン化して保存 itemCode={}", product.getItemCode());
                } else {
                    log.debug("[normalize] LLM 実行したが差分なし itemCode={}", product.getItemCode());
                }

            } else {
                // desc はOK。caption だけ占位なら補正して保存
                final String fixedCaption = fixCaptionIfNeeded(product.getItemCaption(), product.getDescriptionPlain());
                if (!equalsSafe(fixedCaption, product.getItemCaption())) {
                    product.setItemCaption(fixedCaption);
                    product = productRepository.save(product);
                    log.info("[normalize] caption を補正して保存 itemCode={}", product.getItemCode());
                } else {
                    log.debug("[normalize] DB命中かつ整形済み → LLM スキップ itemCode={}", product.getItemCode());
                }
            }
        }

        return ResponseEntity.ok(buildSuccessResponse(product));
    }

    @Override
    public ResponseEntity<Boolean> isPurchasable(String itemCode) {
        return productRepository.findByItemCode(itemCode)
                .map(p -> ResponseEntity.ok(p.getInventory() != null && p.getInventory() > 0))
                .orElseGet(() -> new ResponseEntity<>(false, HttpStatus.BAD_REQUEST));
    }

    /**
     * 説明文（desc）が LLM 整形を要するかを判定する。
     * caption はここでは見ない（caption の占位は別途補正する）。
     */
    private boolean needsCleanDesc(String html, String plain) {
        // 入力が乏しい場合
        if (!StringUtils.hasText(html) && !StringUtils.hasText(plain)) {
            return true;
        }
        // 占位・ノイズが混在
        final String target = (html == null ? "" : html) + "\n" + (plain == null ? "" : plain);
        for (String b : BANNED_PHRASES) {
            if (target.contains(b)) return true;
        }
        // 想定のセクション構造なら整形不要
        if (StringUtils.hasText(html)
                && html.contains("desc-section")
                && (html.contains("<p>") || html.contains("<ul") || html.contains("<table"))) {
            // <li>先頭に装飾記号があれば再整形
            if (BULLET_DECOR.matcher(html).find()) return true;
            // <section> で開始しない（先頭にゴミ）なら再整形
            if (!html.trim().startsWith("<section")) return true;
            return false;
        }
        // それ以外は整形した方が安全
        return true;
    }

    /** caption が空/占位なら、plain から要約を作って返す。問題なければ元の caption を返す。 */
    private String fixCaptionIfNeeded(String caption, String plainSource) {
        if (isBlankOrBanned(caption)) {
            String candidate = (plainSource == null) ? "" : plainSource.trim();
            // 1行目または先頭80〜120文字程度で要約を作る
            String firstLine = candidate.contains("\n") ? candidate.substring(0, candidate.indexOf('\n')).trim() : candidate;
            if (firstLine.length() > 120) firstLine = firstLine.substring(0, 120).trim();
            return StringUtils.hasText(firstLine) ? firstLine : "";
        }
        return caption;
    }

    private boolean isBlankOrBanned(String s) {
        if (!StringUtils.hasText(s)) return true;
        for (String b : BANNED_PHRASES) {
            if (s.contains(b)) return true;
        }
        return false;
    }

    private boolean equalsSafe(String a, String b) {
        return (a == b) || (a != null && a.equals(b));
    }

    private ProductDetailResponseDto buildSuccessResponse(Product product) {
        Double score = reviewRepository.findAverageRatingByItemCode(product.getItemCode());
        int reviewCount = reviewRepository.countByProductItemCodeAndDeletedFalse(product.getItemCode());

        ProductDetailResponseDto.ProductDto dto = ProductDetailResponseDto.ProductDto.builder()
                .itemCode(product.getItemCode())
                .itemName(product.getItemName())
                .itemCaption(product.getItemCaption()) // 補正済み caption を返す
                .catchcopy(product.getCatchcopy())
                .score(score != null ? Math.round(score * 10.0) / 10.0 : 0.0)
                .reviewCount(reviewCount)
                .price(product.getPrice())
                .imageUrls(product.getImages() != null ? product.getImages() : List.of())
                .descriptionPlain(product.getDescriptionPlain())
                .descriptionHtml(product.getDescriptionHtml())
                .build();

        return ProductDetailResponseDto.builder()
                .message("success")
                .product(dto)
                .build();
    }

    private ProductDetailResponseDto buildFailResponse() {
        return ProductDetailResponseDto.builder()
                .message("fail")
                .product(null)
                .build();
    }
}
