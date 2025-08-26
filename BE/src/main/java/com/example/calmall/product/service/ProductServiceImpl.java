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

    // ---- Groq fallback 檢測常數 ----
    private static final String GROQ_FALLBACK_MARKER = "<!--__GROQ_FALLBACK__-->";
    private static final String GROQ_FALLBACK_PHRASE = "Groq の1日あたりのトークン上限を超過しました";

    private static boolean containsFallback(String s) {
        return s != null && (s.contains(GROQ_FALLBACK_MARKER) || s.contains(GROQ_FALLBACK_PHRASE));
    }

    private static boolean isGroqFallback(String html, String plain, String caption) {
        return containsFallback(html) || containsFallback(plain) || containsFallback(caption);
    }

    private static String stripFallbackAll(String s) {
        if (s == null) return null;
        String t = s.replace(GROQ_FALLBACK_MARKER, "")
                .replace("（" + GROQ_FALLBACK_PHRASE + "）", "")
                .replace("の（" + GROQ_FALLBACK_PHRASE + "）", "")
                .replace(GROQ_FALLBACK_PHRASE, "")
                .trim();
        // 可能殘留的空 <section> 或連續 section，交給前端渲染沒事；這裡不做重排。
        return t;
    }

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
                // 入力なし → 簡易説明
                String fallback = DescriptionFallbackBuilder.buildFromMeta(
                        product.getItemName(),
                        product.getImages() == null ? 0 : product.getImages().size()
                );
                product.setDescriptionHtml(fallback);
                product.setDescriptionPlain(DescriptionHtmlToPlain.toPlain(fallback));
                log.debug("[normalize] 入力テキスト無し → 簡易説明を生成 itemCode={}", itemCode);

            } else if (needsClean(product)) {
                // LLM 整形開始（有 fallback 訊號時會在 Facade 內短路）
                log.debug("[normalize] 新規取得 → LLM 整形開始 itemCode={}", itemCode);
                final String cleanHtml = descriptionCleanerFacade.buildCleanHtml(
                        product.getDescriptionHtml(),
                        product.getDescriptionPlain(),
                        product.getItemCaption(),
                        product.getItemName()
                );
                final String cleanPlain = DescriptionHtmlToPlain.toPlain(cleanHtml);

                // === Groq fallback：只回不存 ===
                if (isGroqFallback(cleanHtml, cleanPlain, product.getItemCaption())) {
                    log.warn("[normalize] Groq quota exceeded → skip saving fallback to DB itemCode={}", itemCode);
                    return ResponseEntity.ok(
                            buildSuccessResponse(
                                    product.toBuilder()
                                            .itemCaption(stripFallbackAll(product.getItemCaption()))
                                            .descriptionHtml(stripFallbackAll(cleanHtml))
                                            .descriptionPlain(stripFallbackAll(cleanPlain))
                                            .build()
                            )
                    );
                }

                product.setDescriptionHtml(cleanHtml);
                product.setDescriptionPlain(cleanPlain);

            } else {
                log.debug("[normalize] 新規取得だが整形不要 itemCode={}", itemCode);
            }

            // caption 補正
            final String fixedCaption = fixCaptionIfNeeded(product.getItemCaption(),
                    product.getDescriptionHtml(), product.getDescriptionPlain());
            if (!equalsSafe(fixedCaption, product.getItemCaption())) {
                product.setItemCaption(fixedCaption);
                log.info("[normalize] caption を補正 itemCode={}", itemCode);
            }

            product = productRepository.save(product);
            log.info("[persist] 楽天APIからの商品を保存 itemCode={}", product.getItemCode());

        } else {
            // 既存レコード
            log.info("[source=DB] 既存商品を取得 itemCode={} name={}", product.getItemCode(), product.getItemName());

            boolean dirty = false;

            // 5) 説明空 → 簡易説明
            if (isAllBlank(product.getDescriptionHtml(), product.getDescriptionPlain(), product.getItemCaption())) {
                String fb = DescriptionFallbackBuilder.buildFromMeta(
                        product.getItemName(),
                        product.getImages() == null ? 0 : product.getImages().size()
                );
                String fbPlain = DescriptionHtmlToPlain.toPlain(fb);

                if (!equalsSafe(fb, product.getDescriptionHtml())) {
                    product.setDescriptionHtml(fb);
                    dirty = true;
                }
                if (!equalsSafe(fbPlain, product.getDescriptionPlain())) {
                    product.setDescriptionPlain(fbPlain);
                    dirty = true;
                }
                log.debug("[normalize] DB商品説明なし → 簡易説明保存予定 itemCode={}", product.getItemCode());

            } else if (needsClean(product)) {
                // 再整形
                log.debug("[normalize] DB命中だが未整形 → LLM 整形開始 itemCode={}", product.getItemCode());
                final String cleanHtml = descriptionCleanerFacade.buildCleanHtml(
                        product.getDescriptionHtml(),
                        product.getDescriptionPlain(),
                        product.getItemCaption(),
                        product.getItemName()
                );
                final String cleanPlain = DescriptionHtmlToPlain.toPlain(cleanHtml);

                // === Groq fallback：只回不存 ===
                if (isGroqFallback(cleanHtml, cleanPlain, product.getItemCaption())) {
                    log.warn("[normalize] Groq quota exceeded → skip saving fallback to DB itemCode={}", itemCode);
                    return ResponseEntity.ok(
                            buildSuccessResponse(
                                    product.toBuilder()
                                            .itemCaption(stripFallbackAll(product.getItemCaption()))
                                            .descriptionHtml(stripFallbackAll(cleanHtml))
                                            .descriptionPlain(stripFallbackAll(cleanPlain))
                                            .build()
                            )
                    );
                }

                if (!equalsSafe(cleanHtml, product.getDescriptionHtml())) {
                    product.setDescriptionHtml(cleanHtml);
                    dirty = true;
                }
                if (!equalsSafe(cleanPlain, product.getDescriptionPlain())) {
                    product.setDescriptionPlain(cleanPlain);
                    dirty = true;
                }

            } else {
                log.debug("[normalize] DB命中かつ整形済み → スキップ itemCode={}", product.getItemCode());
            }

            // caption 補正
            final String fixedCaption = fixCaptionIfNeeded(product.getItemCaption(),
                    product.getDescriptionHtml(), product.getDescriptionPlain());
            if (!equalsSafe(fixedCaption, product.getItemCaption())) {
                product.setItemCaption(fixedCaption);
                dirty = true;
            }

            if (dirty) {
                product = productRepository.save(product);
                log.info("[persist] DB更新保存 itemCode={}", product.getItemCode());
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

    // === Helper ===

    private static boolean needsClean(Product p) {
        final String html = p.getDescriptionHtml();
        final String plain = p.getDescriptionPlain();
        final String caption = p.getItemCaption();

        if (isAllBlank(html, plain, caption)) return false;

        // fallback 文字混入 → 需要清理（但會在 Facade 內短路，不再丟給 LLM）
        if (containsFallback(html) || containsFallback(plain) || containsFallback(caption)) return true;

        final String[] banned = {
                "入力が必要", "please provide input", "no input provided", "placeholder", "これはテストです"
        };
        String all = (html == null ? "" : html) + "\n" +
                (plain == null ? "" : plain) + "\n" +
                (caption == null ? "" : caption);
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
        return !StringUtils.hasText(html) &&
                !StringUtils.hasText(plain) &&
                !StringUtils.hasText(caption);
    }

    // === caption 処理 ===
    private String fixCaptionIfNeeded(String current, String html, String plain) {
        if (!isBadCaption(current)) return stripFallbackAll(current);

        String picked = pickCaptionFromHtml(html);
        if (!StringUtils.hasText(picked)) picked = pickCaptionFromPlain(plain);
        if (!StringUtils.hasText(picked)) return "";

        picked = picked.trim();
        if (picked.length() > 120) picked = picked.substring(0, 120).trim();
        return stripFallbackAll(picked);
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
        return low.contains("入力が必要") || low.contains("provide input") || low.contains("placeholder")
                || containsFallback(t);
    }

    private static String findFirstTagText(String html, String sectionRegex, String tag) {
        String target = html;
        if (StringUtils.hasText(sectionRegex)) {
            Pattern sec = Pattern.compile(sectionRegex + "(.*?)</section>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher ms = sec.matcher(html);
            if (ms.find()) target = ms.group(1);
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

    // === Response Builder ===
    private ProductDetailResponseDto buildSuccessResponse(Product product) {
        Double score = reviewRepository.findAverageRatingByItemCode(product.getItemCode());
        int reviewCount = reviewRepository.countByProductItemCodeAndDeletedFalse(product.getItemCode());

        // 回應前淨化，避免 marker/提示句外洩
        String safeCaption = stripFallbackAll(product.getItemCaption());
        String safeHtml    = stripFallbackAll(product.getDescriptionHtml());
        String safePlain   = stripFallbackAll(product.getDescriptionPlain());

        ProductDetailResponseDto.ProductDto dto = ProductDetailResponseDto.ProductDto.builder()
                .itemCode(product.getItemCode())
                .itemName(product.getItemName())
                .itemCaption(safeCaption)
                .catchcopy(product.getCatchcopy())
                .score(score != null ? Math.round(score * 10.0) / 10.0 : 0.0)
                .reviewCount(reviewCount)
                .price(product.getPrice())
                .imageUrls(product.getImages() != null ? product.getImages() : List.of())
                .descriptionPlain(safePlain)
                .descriptionHtml(safeHtml)
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
