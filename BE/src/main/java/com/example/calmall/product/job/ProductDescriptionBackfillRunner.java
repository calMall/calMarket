package com.example.calmall.product.job;

import com.example.calmall.product.entity.Product;
import com.example.calmall.product.repository.ProductRepository;
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

    // 起動時に実行するかどうか（true のときだけ実行）
    @Value("${backfill.product-description:false}")
    private boolean enabled;

    // ページサイズ
    @Value("${backfill.page-size:500}")
    private int pageSize;

    // itemCaption も HTML（超クリーン）へ上書き保存するか
    @Value("${backfill.persist-item-caption:true}")
    private boolean persistItemCaption;

    @Override
    public void run(String... args) {
        if (!enabled) {
            log.info("[Backfill] disabled");
            return;
        }

        int page = 0;
        int updated = 0;
        int processed = 0;

        while (true) {
            Page<Product> p = productRepository.findAll(PageRequest.of(page, pageSize));
            if (p.isEmpty()) break;

            for (Product prod : p.getContent()) {
                try {
                    // 1) HTML / Plain / Caption を入力に「超・クリーン化」
                    String cleanHtml  = DescriptionSuperCleaner.buildCleanHtml(
                            prod.getDescriptionHtml(),
                            prod.getDescriptionPlain(),
                            prod.getItemCaption()
                    );
                    String cleanPlain = DescriptionSuperCleaner.toPlain(cleanHtml);

                    // 2) 差分がある場合のみ更新
                    boolean dirty = false;

                    if (!equalsSafe(cleanHtml, prod.getDescriptionHtml())) {
                        prod.setDescriptionHtml(cleanHtml);
                        dirty = true;
                    }
                    if (!equalsSafe(cleanPlain, prod.getDescriptionPlain())) {
                        prod.setDescriptionPlain(cleanPlain);
                        dirty = true;
                    }
                    // itemCaption を HTML（超クリーン）で保存
                    if (persistItemCaption && !equalsSafe(cleanHtml, prod.getItemCaption())) {
                        prod.setItemCaption(cleanHtml);
                        dirty = true;
                    }

                    if (dirty) {
                        productRepository.save(prod);
                        updated++;
                    }
                    processed++;
                } catch (Exception e) {
                    log.warn("[Backfill] normalize failed itemCode={} : {}", prod.getItemCode(), e.getMessage());
                }
            }

            if (!p.hasNext()) break;
            page++;
        }

        log.info("[Backfill] processed={} updated={} pageSize={}", processed, updated, pageSize);
    }

    private boolean equalsSafe(String a, String b) {
        return (a == b) || (a != null && a.equals(b));
    }
}
