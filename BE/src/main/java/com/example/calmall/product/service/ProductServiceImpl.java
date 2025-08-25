package com.example.calmall.product.service;

import com.example.calmall.product.dto.ProductDetailResponseDto;
import com.example.calmall.product.entity.Product;
import com.example.calmall.product.repository.ProductRepository;
import com.example.calmall.product.text.DescriptionCleanerFacade;
import com.example.calmall.product.text.DescriptionFallbackBuilder;
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

            // 3) 初回保存前に説明文を決定
            if (isAllBlank(product.getDescriptionHtml(), product.getDescriptionPlain(), product.getItemCaption())) {
                // テキストが完全に無い → LLMは使わず簡易説明を生成
                String fallback = DescriptionFallbackBuilder.buildFromMeta(
                        product.getItemName(),
                        product.getImages() == null ? 0 : product.getImages().size()
                );
                product.setDescriptionHtml(fallback);
                product.setDescriptionPlain(DescriptionHtmlToPlain.toPlain(fallback));
                log.debug("[normalize] 入力テキスト無し → 画像点数と商品名で簡易説明を生成 itemCode={}", itemCode);

            } else if (needsClean(product)) {
                // テキストはあるが未整形 → LLM
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

            // 4) caption 補正（本文/プレーンから拾う）
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

            // 5) 説明が全て空なら簡易説明を生成して保存
            if (isAllBlank(product.getDescriptionHtml(), product.getDescriptionPlain(), product.getItemCaption())) {
                String fallback = DescriptionFallbackBuilder.buildFromMeta(
                        product.getItemName(),
                        product.getImages() == null ? 0 : product.getImages().size()
                );
                String fallbackPlain = DescriptionHtmlToPlain.toPlain(fallback);

                if (!equalsSafe(fallback, product.getDescriptionHtml())) {
                    product.setDescriptionHtml(fallback);
                    dirty = true;
                }
                if (!equalsSafe(fallbackPlain, product.getDescriptionPlain())) {
                    product.setDescriptionPlain(fallbackPlain);
                    dirty = true;
                }
                log.debug("[normalize] 入力テキスト無し → 簡易説明を保存予定 itemCode={}", product.getItemCode());

            } else if (needsClean(product)) {
                // 6) 必要なら LLM で再整形
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

            // 7) caption 補正（見出し語や空なら本文から再生成）
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
     * LLM 整形が必要かを判定。
     * ※ 全ソース空の場合は上位で簡易説明に切替えるので、ここでは扱わない。
     */
    private static boolean needsClean(Product p) {
        final String html = p.getDescriptionHtml();
        final String plain = p.getDescriptionPlain();
        final String caption = p.getItemCaption();

        // 全部空 → ここでは LLM 不要扱い（上位で簡易説明へ）
        if (isAllBlank(html, plain, caption)) {
            return false;
        }

        // プレースホルダ／不適切ワードが含まれる場合は整形
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

        // HTML 構造の簡易判定
        if (StringUtils.hasText(html)
                && html.contains("desc-section")
                && (html.contains("<p>") || html.contains("<ul") || html.contains("<table"))) {
            if (Pattern.compile("<li>\\s*[・●•\\-*]").matcher(html).find()) return true; // 箇条書きの飾り記号を除去したい
            if (!html.trim().startsWith("<section")) return true;                        // 先頭にゴミ文字
            return false;
        }
        return true; // それ以外は未整形とみなす
    }

    // 3ソース（HTML/プレーン/キャプション）がすべて空か
    private static boolean isAllBlank(String html, String plain, String caption) {
        return !StringUtils.hasText(html)
                && !StringUtils.hasText(plain)
                && !StringUtils.hasText(caption);
    }


    // captionを必要に応じて補正する。
    private String fixCaptionIfNeeded(String current, String html, String plain) {
        if (!isBadCaption(current)) return current;

        String picked = pickCaptionFromHtml(html);
        if (!StringUtils.hasText(picked)) {
            picked = pickCaptionFromPlain(plain);
        }
        if (!StringUtils.hasText(picked)) return ""; // 拾えない場合は空

        picked = picked.trim();
        if (picked.length() > 120) picked = picked.substring(0, 120).trim();
        return picked;
    }

    // HTMLから本文の最初の自然文を拾う
    private static String pickCaptionFromHtml(String html) {
        if (!StringUtils.hasText(html)) return "";

        // body セクション内の <p> を優先
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

    // captionとして不適か判定
    private static boolean isBadCaption(String s) {
        if (!StringUtils.hasText(s)) return true;
        String t = normalizeOneLine(s);
        if (t.length() <= 2) return true;                  // 短すぎ
        if (HEADING_ONLY.matcher(t).matches()) return true; // 見出しだけ
        String low = t.toLowerCase();
        if (low.contains("入力が必要") || low.contains("provide input") || low.contains("placeholder")) return true;
        return false;
    }

    // HTMLから最初のタグ内容を抽出
    private static String findFirstTagText(String html, String sectionRegex, String tag) {
        String target = html;
        if (StringUtils.hasText(sectionRegex)) {
            Pattern sec = Pattern.compile(sectionRegex + "(.*?)</section>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher ms = sec.matcher(html);
            if (ms.find()) {
                target = ms.group(1);
            } else {
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

    // 1行化記号を除去
    private static String normalizeOneLine(String s) {
        if (s == null) return "";
        return s.replace('\u00A0', ' ')
                .replace('\u3000', ' ')
                .replaceAll("\\s+", " ")
                .replaceAll("^[・●•\\-*\\s]+", "")
                .trim();
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
                .itemCaption(product.getItemCaption())
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
