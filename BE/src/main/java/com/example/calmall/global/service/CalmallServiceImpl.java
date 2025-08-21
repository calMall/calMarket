package com.example.calmall.global.service;

import com.example.calmall.cartitem.entity.CartItem;
import com.example.calmall.cartitem.repository.CartItemRepository;
import com.example.calmall.orders.entity.Orders;
import com.example.calmall.orders.repository.OrdersRepository;
import com.example.calmall.product.entity.Product;
import com.example.calmall.product.repository.ProductRepository;
import com.example.calmall.review.entity.Review;
import com.example.calmall.review.repository.ReviewRepository;
import com.example.calmall.reviewLike.entity.ReviewLike;
import com.example.calmall.reviewLike.repository.ReviewLikeRepository;
import com.example.calmall.user.entity.User;
import com.example.calmall.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.*;

/**
 * ECサイトの共通サービス処理（取得系・外部連携）を担当するサービスクラス
 */
@Service
@RequiredArgsConstructor
public class CalmallServiceImpl implements CalmallService {

    @Value("${rakuten.app.id}")
    private String appId; // 楽天アプリID（application.propertiesから読み込む）

    private final ProductRepository productRepository;
    private final OrdersRepository ordersRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;

    private final RestTemplate restTemplate = new RestTemplate(); // HTTPクライアント

    // 楽天APIから商品情報を取得し、DBに保存（初回のみ）
    @Override
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

    // 商品一覧取得
    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // 商品登録
    @Override
    public Product addProduct(Product product) {
        return productRepository.save(product);
    }

    // ユーザー登録
    @Override
    public User registerUser(User user) {
        return userRepository.save(user);
    }

    // ユーザー全取得
    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // 注文登録
    @Override
    public Orders createOrder(Orders order) {
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus("CREATED");
        return ordersRepository.save(order);
    }

    // 注文全取得
    @Override
    public List<Orders> getAllOrders() {
        return ordersRepository.findAll();
    }

    // カート追加
    @Override
    public CartItem addToCart(CartItem item) {
        return cartItemRepository.save(item);
    }

    // レビュー投稿
    @Override
    public Review postReview(Review review) {
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());
        return reviewRepository.save(review);
    }

    // レビューいいね
    @Override
    public ReviewLike likeReview(ReviewLike like) {
        return reviewLikeRepository.save(like);
    }
}
