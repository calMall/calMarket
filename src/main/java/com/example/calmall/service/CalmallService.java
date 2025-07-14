package com.example.calmall.service;

import com.example.calmall.entity.*;
import com.example.calmall.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.*;

/**
 * ECサイトの全体サービスロジックを管理するサービスクラス
 * - 商品登録、注文作成、レビュー登録、楽天API連携などを含む
 */
@Service
@RequiredArgsConstructor
public class CalmallService {

    @Value("${rakuten.app.id}")
    private String appId; // 楽天アプリID（application.propertiesから読み込む）

    private final ProductRepository productRepository;
    private final OrdersRepository ordersRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;

    private final RestTemplate restTemplate = new RestTemplate(); // HTTPクライアント

    // ------------------------
    // 🔹 楽天API連携機能
    // ------------------------

    /**
     * itemCodeをもとに楽天APIから商品情報を取得し、DBに保存（初回のみ）
     * @param itemCode 楽天の商品コード（例: book:123456）
     * @return 商品エンティティ
     */
    public Product getOrInsertRakutenProduct(String itemCode) {
        Optional<Product> existing = productRepository.findByItemCode(itemCode);
        if (existing.isPresent()) return existing.get();

        String url = UriComponentsBuilder
                .fromUriString("https://app.rakuten.co.jp/services/api/IchibaItem/Search/20170706")
                .queryParam("applicationId", appId)
                .queryParam("format", "json")
                .queryParam("itemCode", itemCode)
                .toUriString();

        JsonNode result = restTemplate.getForObject(url, JsonNode.class);
        JsonNode items = result.get("Items");
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("商品が見つかりません: " + itemCode);
        }

        JsonNode item = items.get(0).get("Item");

        Product product = new Product();
        product.setItemCode(itemCode);
        product.setItemName(item.get("itemName").asText());
        product.setPrice(item.get("itemPrice").asInt());
        product.setImages(List.of(item.get("mediumImageUrls").get(0).get("imageUrl").asText()));
        product.setInventory(new Random().nextInt(20) + 1); // ランダム在庫
        product.setStatus(true);
        product.setCreatedAt(LocalDateTime.now());

        return productRepository.save(product);
    }

    // ------------------------
    // 🔹 商品関連機能
    // ------------------------

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product addProduct(Product product) {
        return productRepository.save(product);
    }

    // ------------------------
    // 🔹 ユーザー機能
    // ------------------------

    public User registerUser(User user) {
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // ------------------------
    // 🔹 注文機能
    // ------------------------

    public Orders createOrder(Orders order) {
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus("CREATED");
        return ordersRepository.save(order);
    }

    public List<Orders> getAllOrders() {
        return ordersRepository.findAll();
    }

    // ------------------------
    // 🔹 カート機能
    // ------------------------

    public CartItem addToCart(CartItem item) {
        return cartItemRepository.save(item);
    }

    // ------------------------
    // 🔹 レビュー機能
    // ------------------------

    public Review postReview(Review review) {
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());
        return reviewRepository.save(review);
    }

    public ReviewLike likeReview(ReviewLike like) {
        return reviewLikeRepository.save(like);
    }
}