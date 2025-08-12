package com.example.calmall.product.job;

import com.example.calmall.product.entity.Product;
import com.example.calmall.product.repository.ProductRepository;
import com.example.calmall.product.text.DescriptionHtmlFormatter;
import com.example.calmall.product.text.JpTextQuickFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductDescriptionBackfillRunner implements CommandLineRunner {

    private final ProductRepository productRepository;

    @Value("${backfill.product-description:false}")
    private boolean enabled; // true のときだけ実行

    @Override
    public void run(String... args) {
        if (!enabled) {
            log.info("[Backfill] disabled");
            return;
        }

        // まとめて全件。件数が多い環境はページングに差し替え
        List<Product> all = productRepository.findAll();

        int updated = 0;
        List<Product> batch = new ArrayList<>(256);

        for (Product p : all) {
            boolean needPlain = p.getDescriptionPlain() == null || p.getDescriptionPlain().isBlank();
            boolean needHtml  = p.getDescriptionHtml()  == null || p.getDescriptionHtml().isBlank();
            if (!needPlain && !needHtml) continue;

            // 元の説明文。HTML実体を戻す（&nbsp; など）
            String raw = p.getItemCaption() == null ? "" : p.getItemCaption();
            raw = HtmlUtils.htmlUnescape(raw);

            // 整形
            String plain = JpTextQuickFormat.toReadablePlain(raw);
            String html  = DescriptionHtmlFormatter.toSafeHtml(raw);

            if (needPlain) p.setDescriptionPlain(plain);
            if (needHtml)  p.setDescriptionHtml(html);

            batch.add(p);
            updated++;

            // バッチ保存
            if (batch.size() >= 200) {
                productRepository.saveAll(batch);
                batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            productRepository.saveAll(batch);
        }

        log.info("[Backfill] updated={} products", updated);
    }
}
