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
        // 既存DBを参照（id を引き継ぐために保持するだけ）
        Product db = productRepository.findByItemCode(itemCode).orElse(null);

        // 1) 毎回まず楽天APIを叩く
        log.info("[forceFetch] 楽天APIを優先取得 itemCode={}", itemCode);
        Product fetched = rakutenApiService.fetchProductFromRakuten(itemCode).orElse(null);

        if (fetched != null) {
            // 2) 説明が全空なら fallback、そうでなければ LLM 整形
            if (isAllBlank(fetched.getDescriptionHtml(), fetched.getDescriptionPlain(), fetched.getItemCaption())) {
                String fallback = DescriptionFallbackBuilder.buildFromMeta(
                        fetched.getItemName(),
                        fetched.getImages() == null ? 0 : fetched.getImages().size()
                );
                fetched.setDescriptionHtml(fallback);
                fetched.setDescriptionPlain(DescriptionHtmlToPlain.toPlain(fallback));
                log.debug("[forceFetch][normalize] 入力テキスト無し → 簡易説明生成 itemCode={}", itemCode);
            } else if (needsClean(fetched)) {
                log.debug("[forceFetch][normalize] LLM 整形開始 itemCode={}", itemCode);
                final String cleanHtml = descriptionCleanerFacade.buildCleanHtml(
                        fetched.getDescriptionHtml(),
                        fetched.getDescriptionPlain(),
                        fetched.getItemCaption()
                );
                final String cleanPlain = DescriptionHtmlToPlain.toPlain(cleanHtml);
                fetched.setDescriptionHtml(cleanHtml);
                fetched.setDescriptionPlain(cleanPlain);
            } else {
                log.debug("[forceFetch][normalize] 整形不要と判定 itemCode={}", itemCode);
            }

            // 3) caption 補正
            final String fixedCaption = fixCaptionIfNeeded(
                    fetched.getItemCaption(),
                    fetched.getDescriptionHtml(),
                    fetched.getDescriptionPlain()
            );
            if (!equalsSafe(fixedCaption, fetched.getItemCaption())) {
                fetched.setItemCaption(fixedCaption);
                log.info("[forceFetch][normalize] caption を補正 itemCode={}", itemCode);
            }

            // 4) 既存があれば id を引き継いで上書き保存
            Product saved = productRepository.save(mergeIdsIfNeeded(db, fetched));
            log.info("[forceFetch] 楽天APIの内容で保存 itemCode={}", saved.getItemCode());
            return ResponseEntity.ok(buildSuccessResponse(saved));
        }

        // ---- API失敗。DBフォールバック ----
        log.warn("[forceFetch] 楽天API取得失敗 → DBへフォールバック itemCode={}", itemCode);
        if (db == null) {
            log.warn("[not-found] DBにも無し itemCode={}", itemCode);
            return new ResponseEntity<>(buildFailResponse(), HttpStatus.BAD_REQUEST);
        }

        // DB がある場合も、説明が全空なら fallback、未整形なら LLM
        boolean dirty = false;
        if (isAllBlank(db.getDescriptionHtml(), db.getDescriptionPlain(), db.getItemCaption())) {
            String fallback = DescriptionFallbackBuilder.buildFromMeta(
                    db.getItemName(),
                    db.getImages() == null ? 0 : db.getImages().size()
            );
            String fallbackPlain = DescriptionHtmlToPlain.toPlain(fallback);

            if (!equalsSafe(fallback, db.getDescriptionHtml())) { db.setDescriptionHtml(fallback); dirty = true; }
            if (!equalsSafe(fallbackPlain, db.getDescriptionPlain())) { db.setDescriptionPlain(fallbackPlain); dirty = true; }
            log.debug("[fallback][normalize] 入力テキスト無し → 簡易説明保存 itemCode={}", db.getItemCode());
        } else if (needsClean(db)) {
            log.debug("[fallback][normalize] DB命中だが未整形/不正規 → LLM 整形開始 itemCode={}", db.getItemCode());
            final String cleanHtml = descriptionCleanerFacade.buildCleanHtml(
                    db.getDescriptionHtml(),
                    db.getDescriptionPlain(),
                    db.getItemCaption()
            );
            final String cleanPlain = DescriptionHtmlToPlain.toPlain(cleanHtml);
            if (!equalsSafe(cleanHtml, db.getDescriptionHtml())) { db.setDescriptionHtml(cleanHtml); dirty = true; }
            if (!equalsSafe(cleanPlain, db.getDescriptionPlain())) { db.setDescriptionPlain(cleanPlain); dirty = true; }
        } else {
            log.debug("[fallback][normalize] DB命中かつ整形済み → LLM スキップ itemCode={}", db.getItemCode());
        }

        // caption 補正
        final String fixedCaption = fixCaptionIfNeeded(db.getItemCaption(), db.getDescriptionHtml(), db.getDescriptionPlain());
        if (!equalsSafe(fixedCaption, db.getItemCaption())) {
            db.setItemCaption(fixedCaption);
            dirty = true;
            log.info("[fallback][normalize] caption を補正して保存 itemCode={}", db.getItemCode());
        }

        if (dirty) {
            db = productRepository.save(db);
            log.info("[fallback][normalize] 説明文をクリーン化して保存 itemCode={}", db.getItemCode());
        }
        return ResponseEntity.ok(buildSuccessResponse(db));
    }

    @Override
    public ResponseEntity<Boolean> isPurchasable(String itemCode) {
        return productRepository.findByItemCode(itemCode)
                .map(p -> ResponseEntity.ok(p.getInventory() != null && p.getInventory() > 0))
                .orElseGet(() -> new ResponseEntity<>(false, HttpStatus.BAD_REQUEST));
    }

    private static Product mergeIdsIfNeeded(Product db, Product fetched) {
        return fetched; // そのまま返す
    }

    // --- 以下、既存のユーティリティ群は変更なし ---

    private static boolean needsClean(Product p) {
        final String html = p.getDescriptionHtml();
        final String plain = p.getDescriptionPlain();
        final String caption = p.getItemCaption();

        if (isAllBlank(html, plain, caption)) {
            return false;
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

    private static boolean isAllBlank(String html, String plain, String caption) {
        return !StringUtils.hasText(html)
                && !StringUtils.hasText(plain)
                && !StringUtils.hasText(caption);
    }

    private String fixCaptionIfNeeded(String current, String html, String plain) {
        if (!isBadCaption(current)) return current;

        String picked = pickCaptionFromHtml(html);
        if (!StringUtils.hasText(picked)) {
            picked = pickCaptionFromPlain(plain);
        }
        if (!StringUtils.hasText(picked)) return "";

        picked = picked.trim();
        if (picked.length() > 120) picked = picked.substring(0, 120).trim();
        return picked;
    }

    private static String pickCaptionFromHtml(String html) {
        if (!StringUtils.hasText(html)) return "";
        String pFromBody = findFirstTagText(html, "<section[^>]*class=\"[^\"]*desc-section\\s*body[^\"]*\"[^>]*>", "p");
        if (StringUtils.hasText(pFromBody) && !isBadCaption(pFromBody)) return normalizeOneLine(pFromBody);
        String pAnywhere = findFirstTagText(html, null, "p");
        if (StringUtils.hasText(pAnywhere) && !isBadCaption(pAnywhere)) return normalizeOneLine(pAnywhere);
        String li = findFirstTagText(html, null, "li");
        if (StringUtils.hasText(li) && !isBadCaption(li)) return normalizeOneLine(li);
        return "";
    }

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

    private static boolean isBadCaption(String s) {
        if (!StringUtils.hasText(s)) return true;
        String t = normalizeOneLine(s);
        if (t.length() <= 2) return true;
        if (HEADING_ONLY.matcher(t).matches()) return true;
        String low = t.toLowerCase();
        if (low.contains("入力が必要") || low.contains("provide input") || low.contains("placeholder")) return true;
        return false;
    }

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
