package com.example.calmall.product.service;

import com.example.calmall.product.dto.ProductDetailResponseDto;
import com.example.calmall.product.entity.Product;
import com.example.calmall.product.repository.ProductRepository;
import com.example.calmall.product.text.DescriptionHtmlFormatter;
import com.example.calmall.product.text.JpTextQuickFormat;
import com.example.calmall.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品情報に関する業務ロジックを実装するサービスクラス
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    // 商品エンティティを扱うリポジトリ
    private final ProductRepository productRepository;

    // 楽天APIから商品情報を取得するサービス
    private final RakutenApiService rakutenApiService;

    // レビュー関連情報の取得に使用
    private final ReviewRepository reviewRepository;

    /**
     * 商品詳細取得処理
     * 1) DBから取得。無ければ楽天APIから取得してDBへ保存
     * 2) 説明文（descriptionPlain / descriptionHtml）が未整備の場合はその場で補完し保存
     * 3) フロント互換のため、最終的に itemCaption を「可読プレーン」に統一して返却
     */
    @Override
    public ResponseEntity<ProductDetailResponseDto> getProductDetail(String itemCode) {

        // --- 1) DB から既存商品を検索 ---
        Product product = productRepository.findByItemCode(itemCode).orElse(null);

        // --- 2) 無ければ 楽天API から取得し保存 ---
        if (product == null) {
            log.info("[source=RakutenAPI] DB未登録 → 楽天API照会 itemCode={}", itemCode);
            product = rakutenApiService.fetchProductFromRakuten(itemCode).orElse(null);
            if (product == null) {
                log.warn("[not found] 楽天APIから取得不可 itemCode={}", itemCode);
                return new ResponseEntity<>(buildFailResponse(), HttpStatus.BAD_REQUEST);
            }
            product = productRepository.save(product);
            log.info("[persist] 楽天APIから取得した商品を保存 itemCode={}", product.getItemCode());
        } else {
            log.info("[source=DB] 既存商品を取得 itemCode={} name={}", product.getItemCode(), product.getItemName());
        }

        // --- 3) 説明文の補完（古いデータや欠落対策）---
        boolean needSave = false;

        // raw 入力源：現状は itemCaption を使用（生テキスト/可読化済みの両方に対応）
        String raw = product.getItemCaption() == null ? "" : product.getItemCaption();

        // descriptionPlain が未設定/空なら、可読なプレーンテキストを生成
        if (isBlank(product.getDescriptionPlain())) {
            String plain = JpTextQuickFormat.toReadablePlain(raw); // 句読点/箇条書き/キー:値を認識して改行整形
            product.setDescriptionPlain(plain);
            needSave = true;
        }

        // descriptionHtml が未設定/空なら、安全なHTMLを生成
        if (isBlank(product.getDescriptionHtml())) {
            String html = DescriptionHtmlFormatter.toSafeHtml(raw); // <table>/<ul>/<p> の最小安全タグのみ
            product.setDescriptionHtml(html);
            needSave = true;
        }

        // ★ フロント互換：常に itemCaption は「可読プレーン」を返す
        String ensuredPlain = !isBlank(product.getDescriptionPlain())
                ? product.getDescriptionPlain()
                : JpTextQuickFormat.toReadablePlain(raw);
        if (!ensuredPlain.equals(product.getItemCaption())) {
            product.setItemCaption(ensuredPlain);
            needSave = true;
        }

        // 補完・上書きが発生した場合は保存
        if (needSave) {
            product = productRepository.save(product);
            log.info("[normalize] 説明文を補完/整形し保存 itemCode={}", product.getItemCode());
        }

        // --- 4) 成功レスポンスを返却 ---
        return ResponseEntity.ok(buildSuccessResponse(product));
    }

    /**
     * 購入可否判定：
     * inventory が 1 以上なら true とする（簡易仕様）
     */
    @Override
    public ResponseEntity<Boolean> isPurchasable(String itemCode) {
        return productRepository.findByItemCode(itemCode)
                .map(p -> {
                    boolean ok = p.getInventory() != null && p.getInventory() > 0;
                    log.info("[purchasable] itemCode={} inventory={} result={}", itemCode, p.getInventory(), ok);
                    return ResponseEntity.ok(ok);
                })
                .orElseGet(() -> {
                    log.warn("[purchasable] 対象商品が存在しません itemCode={}", itemCode);
                    return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
                });
    }

    // -------------------- private helpers --------------------

    // 成功レスポンスDTOの組み立て（itemCaption は可読版、description も含める）
    private ProductDetailResponseDto buildSuccessResponse(Product product) {
        // 平均スコア（null対策付）とレビュー件数（削除済み除外）を取得
        Double score = reviewRepository.findAverageRatingByItemCode(product.getItemCode());
        int reviewCount = reviewRepository.countByProductItemCodeAndDeletedFalse(product.getItemCode());

        ProductDetailResponseDto.ProductDto dto = ProductDetailResponseDto.ProductDto.builder()
                .itemCode(product.getItemCode())
                .itemName(product.getItemName())
                .itemCaption(product.getItemCaption()) // フロントはこれを text として使用
                .catchcopy(product.getCatchcopy())
                .score(score != null ? Math.round(score * 10.0) / 10.0 : 0.0) // 小数第1位で四捨五入
                .reviewCount(reviewCount)
                .price(product.getPrice())
                .imageUrls(product.getImages() != null ? product.getImages() : List.of())
                // 将来フロントで使う想定（現在は互換のため itemCaption だけで十分）
                .descriptionPlain(product.getDescriptionPlain())
                .descriptionHtml(product.getDescriptionHtml())
                .build();

        return ProductDetailResponseDto.builder()
                .message("success")
                .product(dto)
                .build();
    }

    // 失敗レスポンスDTOの組み立て
    private ProductDetailResponseDto buildFailResponse() {
        return ProductDetailResponseDto.builder()
                .message("fail")
                .product(null)
                .build();
    }

    // 文字列の空判定（null/空白のみを true）
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
