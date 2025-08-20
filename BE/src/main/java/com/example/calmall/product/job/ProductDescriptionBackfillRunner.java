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

    @Value("${backfill.product-description:false}")
    private boolean enabled;

    @Value("${backfill.page-size:500}")
    private int pageSize;

    @Value("${backfill.persist-item-caption:true}")
    private boolean persistItemCaption;

    @Value("${backfill.mode:clean}") // clean | refetch
    private String mode;

    @Override
    public void run(String... args) {
        if (!enabled) {
            log.info("[Backfill] disabled");
            return;
        }

        int page = 0, updated = 0, processed = 0, refetched = 0;

        log.info("[Backfill] start pageSize={} mode={} persistItemCaption={}",
                pageSize, mode, persistItemCaption);

        while (true) {
            Page<Product> p = productRepository.findAll(PageRequest.of(page, pageSize));
            if (p.isEmpty()) break;

            for (Product prod : p.getContent()) {
                try {
                    processed++;

                    Product source = prod;

                    // mode = refetch → 先打 API 取新資料
                    if ("refetch".equalsIgnoreCase(mode)) {
                        var freshOpt = rakutenApiService.fetchProductFromRakuten(prod.getItemCode());
                        if (freshOpt.isPresent()) {
                            source = freshOpt.get();
                            refetched++;
                        } else {
                            log.warn("[Backfill] refetch failed itemCode={}", prod.getItemCode());
                        }
                    }

                    // 共通：跑 DescriptionSuperCleaner
                    String cleanHtml = DescriptionSuperCleaner.buildCleanHtml(
                            source.getDescriptionHtml(),
                            source.getDescriptionPlain(),
                            source.getItemCaption()
                    );
                    String cleanPlain = DescriptionSuperCleaner.toPlain(cleanHtml);

                    boolean dirty = false;

                    if (!cleanHtml.equals(prod.getDescriptionHtml())) {
                        prod.setDescriptionHtml(cleanHtml);
                        dirty = true;
                    }
                    if (!cleanPlain.equals(prod.getDescriptionPlain())) {
                        prod.setDescriptionPlain(cleanPlain);
                        dirty = true;
                    }
                    if (persistItemCaption && !cleanHtml.equals(prod.getItemCaption())) {
                        prod.setItemCaption(cleanHtml);
                        dirty = true;
                    }

                    if (dirty) {
                        productRepository.save(prod);
                        updated++;
                    }

                } catch (Exception e) {
                    log.warn("[Backfill] failed itemCode={} : {}", prod.getItemCode(), e.getMessage());
                }
            }

            if (!p.hasNext()) break;
            page++;
        }

        log.info("[Backfill] done processed={} updated={} refetched={}",
                processed, updated, refetched);
    }
}
