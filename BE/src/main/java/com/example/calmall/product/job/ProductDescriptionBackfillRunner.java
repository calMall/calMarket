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
    private final RakutenApiService rakutenApiService;

    // 起動時に実行するか
    @Value("${backfill.product-description:false}")
    private boolean enabled;

    // ページサイズ
    @Value("${backfill.page-size:500}")
    private int pageSize;

    // itemCaption を HTML で保存するか
    @Value("${backfill.persist-item-caption:true}")
    private boolean persistItemCaption;

    // backfill.mode = clean | refetch
    @Value("${backfill.mode:clean}")
    private String mode;

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

        log.info("[Backfill] start pageSize={} mode={} persistItemCaption={}",
                pageSize, mode, persistItemCaption);

        while (true) {
            Page<Product> p = productRepository.findAll(PageRequest.of(page, pageSize));
            if (p.isEmpty()) break;

            for (Product prod : p.getContent()) {
                try {
                    processed++;

                    Product source = prod;

                    // --- mode=refetch の場合、楽天API再取得 ---
                    if ("refetch".equalsIgnoreCase(mode)) {
                        var freshOpt = rakutenApiService.fetchProductFromRakuten(prod.getItemCode());
                        if (freshOpt.isPresent()) {
                            source = freshOpt.get();
                            refetched++;
                        } else {
                            log.warn("[Backfill] refetch failed itemCode={}", prod.getItemCode());
                        }
                    }

                    // --- SuperCleaner で再整形 ---
                    String cleanHtml = DescriptionSuperCleaner.buildCleanHtml(
                            source.getDescriptionHtml(),
                            source.getDescriptionPlain(),
                            source.getItemCaption()
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
                            log.debug("[Backfill][Updated] itemCode={} len(html)={}",
                                    prod.getItemCode(), cleanHtml.length());
                        }
                    }

                } catch (Exception e) {
                    log.warn("[Backfill] failed itemCode={} : {}", prod.getItemCode(), e.getMessage());
                }
            }

            if (!p.hasNext()) break;
            page++;
        }

        log.info("[Backfill] done processed={} updated={} refetched={} pageSize={} mode={}",
                processed, updated, refetched, pageSize, mode);
    }

    private boolean equalsSafe(String a, String b) {
        return (a == b) || (a != null && a.equals(b));
    }
}
