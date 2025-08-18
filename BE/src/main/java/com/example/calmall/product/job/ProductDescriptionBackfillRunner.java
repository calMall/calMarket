package com.example.calmall.product.job;

import com.example.calmall.product.entity.Product;
import com.example.calmall.product.repository.ProductRepository;
import com.example.calmall.product.service.RakutenApiService;
import com.example.calmall.product.text.DescriptionSuperCleaner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductDescriptionBackfillRunner implements CommandLineRunner {

    private final ProductRepository productRepository;

    // 楽天API 再取得に使用
    private final RakutenApiService rakutenApiService;

    // 起動時に実行するか（true のときだけ実行）
    @Value("${backfill.product-description:false}")
    private boolean enabled;

    // ページサイズ
    @Value("${backfill.page-size:500}")
    private int pageSize;

    // itemCaption を HTML（超クリーン）で保存するか
    // フロントが dangerouslySetInnerHTML で描画する前提なら true 推奨
    @Value("${backfill.persist-item-caption:true}")
    private boolean persistItemCaption;

    // 説明の文字数がこの閾値未満なら「古い/短い」とみなして再取得を試みる（0 で無効）
    @Value("${backfill.refetch-threshold:200}")
    private int refetchThreshold;

    // 常に楽天APIから再取得して上書き（検証用）。true だと全件叩くので注意
    @Value("${backfill.refetch-always:false}")
    private boolean refetchAlways;

    @Override
    public void run(String... args) {
        if (!enabled) {
            log.info("[Backfill] disabled");
            return;
        }

        int page = 0;
        int updated = 0;
        int processed = 0;
        int refetched = 0;

        log.info("[Backfill] start pageSize={} refetchThreshold={} refetchAlways={} persistItemCaption={}",
                pageSize, refetchThreshold, refetchAlways, persistItemCaption);

        while (true) {
            Page<Product> p = productRepository.findAll(PageRequest.of(page, pageSize));
            if (p.isEmpty()) break;

            for (Product prod : p.getContent()) {
                try {
                    processed++;

                    String html0 = nz(prod.getDescriptionHtml());
                    String plain0 = nz(prod.getDescriptionPlain());
                    String cap0 = nz(prod.getItemCaption());

                    // 再取得が必要か判定
                    boolean needsRefetch = refetchAlways
                            || isTooShort(html0, plain0, cap0, refetchThreshold);

                    if (needsRefetch) {
                        var freshOpt = rakutenApiService.fetchProductFromRakuten(prod.getItemCode());
                        if (freshOpt.isPresent()) {
                            Product fresh = freshOpt.get();
                            // 取得直後の Product は SuperCleaner 済みのはず（あなたの実装準拠）
                            // 念のためもう一度最終整形
                            String cleanHtml = DescriptionSuperCleaner.buildCleanHtml(
                                    fresh.getDescriptionHtml(),
                                    fresh.getDescriptionPlain(),
                                    fresh.getItemCaption()
                            );
                            String cleanPlain = DescriptionSuperCleaner.toPlain(cleanHtml);

                            prod.setDescriptionHtml(cleanHtml);
                            prod.setDescriptionPlain(cleanPlain);
                            if (persistItemCaption) {
                                prod.setItemCaption(cleanHtml); // フロントは HTML を描画
                            }

                            productRepository.save(prod);
                            updated++;
                            refetched++;

                            if (log.isDebugEnabled()) {
                                log.debug("[Backfill][Refetched] itemCode={} len(html)={}→{}",
                                        prod.getItemCode(), html0.length(), cleanHtml.length());
                            }
                            continue; // 再整形・保存済みなので次へ
                        } else {
                            log.warn("[Backfill] refetch failed itemCode={}", prod.getItemCode());
                            // 失敗した場合は従来どおりローカル値で整形にフォールバック
                        }
                    }

                    // ここからは DB 内テキストを最終整形するだけ（再取得なしケース）
                    String cleanHtml = DescriptionSuperCleaner.buildCleanHtml(
                            prod.getDescriptionHtml(),
                            prod.getDescriptionPlain(),
                            prod.getItemCaption()
                    );
                    String cleanPlain = DescriptionSuperCleaner.toPlain(cleanHtml);

                    boolean dirty = false;

                    if (!equalsSafe(cleanHtml, prod.getDescriptionHtml())) {
                        prod.setDescriptionHtml(cleanHtml);
                        dirty = true;
                    }
                    if (!equalsSafe(cleanPlain, prod.getDescriptionPlain())) {
                        prod.setDescriptionPlain(cleanPlain);
                        dirty = true;
                    }
                    if (persistItemCaption && !equalsSafe(cleanHtml, prod.getItemCaption())) {
                        prod.setItemCaption(cleanHtml);
                        dirty = true;
                    }

                    if (dirty) {
                        productRepository.save(prod);
                        updated++;
                        if (log.isDebugEnabled()) {
                            log.debug("[Backfill][Normalized] itemCode={} len(html)={}→{}",
                                    prod.getItemCode(), html0.length(), cleanHtml.length());
                        }
                    }

                } catch (Exception e) {
                    log.warn("[Backfill] failed itemCode={} : {}", prod.getItemCode(), e.getMessage());
                }
            }

            if (!p.hasNext()) break;
            page++;
        }

        log.info("[Backfill] done processed={} updated={} refetched={} pageSize={}",
                processed, updated, refetched, pageSize);
    }

    private boolean isTooShort(String html, String plain, String cap, int threshold) {
        if (threshold <= 0) return false;
        int maxLen = Math.max(Math.max(nz(html).length(), nz(plain).length()), nz(cap).length());
        return maxLen < threshold;
    }

    private String nz(String s) {
        return (s == null) ? "" : s;
    }

    private boolean equalsSafe(String a, String b) {
        return (a == b) || (a != null && a.equals(b));
    }
}
