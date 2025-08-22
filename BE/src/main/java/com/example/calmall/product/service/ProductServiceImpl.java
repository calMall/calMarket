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
import java.util.regex.Matcher;
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

    // 見出しだけの文字列を判定（caption には不適）
    private static final Pattern HEADING_ONLY = Pattern.compile(
            "^(素材・成分|成分|素材|仕様|スペック|サイズ|内容|セット内容|特徴|使い方|注意事項|ご注意|JAN|JANコード)\\s*$"
    );

    @Override
    public ResponseEntity<ProductDetailResponseDto> getProductDetail(String itemCode) {
        // 1) DB を参照
        Product product = productRepository.findByItemCode(itemCode).orElse(null);

        if (product == null) {
            // 2) DB に無ければ楽天 API
            log.info("[source=RakutenAPI] DB未登録 → 楽天API照会 itemCode={}", itemCode);
            product = rakutenApiService.fetchProductFromRakuten(itemCode).orElse(null);
            if (product == null) {
                log.warn("[not-found] 楽天APIから取得不可 itemCode={}", itemCode);
                return new ResponseEntity<>(buildFailResponse(), HttpStatus.BAD_REQUEST);
            }

            // 3) 初回保存前に必要なら LLM 整形
            if (needsClean(product)) {
                log.debug("[normalize] 新規取得 → LLM 整形開始 itemCode={}", itemCode);
                final String cleanHtml = descriptionCleanerFacade.buildCleanHtml(
                        product.getDescriptionHtml(),
                        product.getDescriptionPlain(),
                        product.getItemCaption()
                );
                final String cleanPlain = DescriptionHtmlToPlain.toPlain(cleanHtml);
                product.setDescriptionHtml(cleanHtml);
                product.setDescriptionPlain(cleanPlain);
            } else {
                log.debug("[normalize] 新規取得だが整形不要と判定 itemCode={}", itemCode);
            }

            // 4) caption 補正（LLM 後の本文から拾う）
            final String fixedCaption = fixCaptionIfNeeded(product.getItemCaption(),
                    product.getDescriptionHtml(), product.getDescriptionPlain());
            if (!equalsSafe(fixedCaption, product.getItemCaption())) {
                product.setItemCaption(fixedCaption);
                log.info("[normalize] caption を補正して保存 itemCode={}", itemCode);
            }

            product = productRepository.save(product);
            log.info("[persist] 楽天APIから取得した商品を保存 itemCode={}", product.getItemCode());

        } else {
            // 既存レコード
            log.info("[source=DB] 既存商品を取得 itemCode={} name={}", product.getItemCode(), product.getItemName());

            boolean dirty = false;

            // 5) 必要なら LLM で再整形
            if (needsClean(product)) {
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
            } else {
                log.debug("[normalize] DB命中かつ整形済み → LLM スキップ itemCode={}", product.getItemCode());
            }

            // 6) caption 補正（見出し語や空なら本文から再生成）
            final String fixedCaption = fixCaptionIfNeeded(product.getItemCaption(),
                    product.getDescriptionHtml(), product.getDescriptionPlain());
            if (!equalsSafe(fixedCaption, product.getItemCaption())) {
                product.setItemCaption(fixedCaption);
                dirty = true;
                log.info("[normalize] caption を補正して保存 itemCode={}", product.getItemCode());
            }

            if (dirty) {
                product = productRepository.save(product);
                log.info("[normalize] 説明文をクリーン化して保存 itemCode={}", product.getItemCode());
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
     * LLM 整形が必要かを判定
     */
    private static boolean needsClean(Product p) {
        final String html = p.getDescriptionHtml();
        final String plain = p.getDescriptionPlain();
        final String caption = p.getItemCaption();

        if (!StringUtils.hasText(html) && !StringUtils.hasText(plain) && !StringUtils.hasText(caption)) {
            return true;
        }

        final String[] banned = {
                "入力が必要です。原文を入力してください。",
                "please provide input",
                "no input provided",
                "placeholder",
                "これはテストです"
        };
        String all = (html == null ? "" : html) + "\n" + (plain == null ? "" : plain) + "\n" + (caption == null ? "" : caption);
        for (String b : banned) {
            if (all.toLowerCase().contains(b.toLowerCase())) return true;
        }

        if (StringUtils.hasText(html)
                && html.contains("desc-section")
                && (html.contains("<p>") || html.contains("<ul") || html.contains("<table"))) {
            if (Pattern.compile("<li>\\s*[・●•\\-*]").matcher(html).find()) return true;
            if (!html.trim().startsWith("<section")) return true;
            return false;
        }
        return true;
    }

    /**
     * caption を必要に応じて補正する。
     * - 既存 caption が空/見出し語/プレースホルダなら本文から生成
     * - 120 文字に丸める
     */
    private String fixCaptionIfNeeded(String current, String html, String plain) {
        if (!isBadCaption(current)) return current;

        String picked = pickCaptionFromHtml(html);
        if (!StringUtils.hasText(picked)) {
            picked = pickCaptionFromPlain(plain);
        }
        if (!StringUtils.hasText(picked)) return ""; // 何も拾えない場合は空で返す

        picked = picked.trim();
        if (picked.length() > 120) picked = picked.substring(0, 120).trim();
        return picked;
    }

    // HTML から本文の最初の自然文を拾う（見出しや箇条書きは避ける）
    private static String pickCaptionFromHtml(String html) {
        if (!StringUtils.hasText(html)) return "";

        // body セクション内の <p> を最優先
        String pFromBody = findFirstTagText(html, "<section[^>]*class=\"[^\"]*desc-section\\s*body[^\"]*\"[^>]*>", "p");
        if (StringUtils.hasText(pFromBody) && !isBadCaption(pFromBody)) return normalizeOneLine(pFromBody);

        // どこかの <p>
        String pAnywhere = findFirstTagText(html, null, "p");
        if (StringUtils.hasText(pAnywhere) && !isBadCaption(pAnywhere)) return normalizeOneLine(pAnywhere);

        // 箇条書きの最初の <li>
        String li = findFirstTagText(html, null, "li");
        if (StringUtils.hasText(li) && !isBadCaption(li)) return normalizeOneLine(li);

        return "";
    }

    // プレーンから先頭の自然文を拾う
    private static String pickCaptionFromPlain(String plain) {
        if (!StringUtils.hasText(plain)) return "";
        String[] lines = plain.replace("\r", "").split("\n");
        for (String ln : lines) {
            String t = normalizeOneLine(ln);
            if (StringUtils.hasText(t) && !isBadCaption(t)) {
                return t;
            }
        }
        return "";
    }

    // 既存 caption として不適か判定
    private static boolean isBadCaption(String s) {
        if (!StringUtils.hasText(s)) return true;
        String t = normalizeOneLine(s);
        if (t.length() <= 2) return true;                 // 短すぎ
        if (HEADING_ONLY.matcher(t).matches()) return true; // 見出しだけ
        // プレースホルダ類
        String low = t.toLowerCase();
        if (low.contains("入力が必要") || low.contains("provide input") || low.contains("placeholder")) return true;
        return false;
    }

    // HTML から最初のタグ内容を抽出（section 条件で範囲を絞ることも可）
    private static String findFirstTagText(String html, String sectionRegex, String tag) {
        String target = html;
        if (StringUtils.hasText(sectionRegex)) {
            Pattern sec = Pattern.compile(sectionRegex + "(.*?)</section>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher ms = sec.matcher(html);
            if (ms.find()) {
                target = ms.group(1);
            } else {
                // 見つからなければ全体を対象に続行
                target = html;
            }
        }
        Pattern ptn = Pattern.compile("<" + tag + "[^>]*>(.*?)</" + tag + ">", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = ptn.matcher(target);
        while (m.find()) {
            String inner = m.group(1)
                    .replaceAll("(?i)<br\\s*/?>", " ")
                    .replaceAll("<[^>]+>", " ")
                    .replace("&nbsp;", " ")
                    .trim();
            inner = normalizeOneLine(inner);
            if (StringUtils.hasText(inner)) return inner;
        }
        return "";
    }

    private static String normalizeOneLine(String s) {
        if (s == null) return "";
        String t = s.replace('\u00A0', ' ')
                .replace('\u3000', ' ')
                .replaceAll("\\s+", " ")
                .replaceAll("^[・●•\\-*\\s]+", "") // 行頭の飾りを除去
                .trim();
        return t;
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
                .itemCaption(product.getItemCaption()) // caption は維持（補正済みを返す）
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
