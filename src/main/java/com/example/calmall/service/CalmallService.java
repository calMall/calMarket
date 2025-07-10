package com.example.calmall.service;

import com.example.calmall.entity.*;
import com.example.calmall.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 各種エンティティに対応したサービスクラス
 */
@Service
@RequiredArgsConstructor
public class CalmallService {

    private final ProductRepository productRepository;
    private final OrdersRepository ordersRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;

    // 商品一覧取得
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // 商品登録
    public Product addProduct(Product product) {
        return productRepository.save(product);
    }

    // ユーザー登録
    public User registerUser(User user) {
        return userRepository.save(user);
    }

    // 注文作成
    public Orders createOrder(Orders order) {
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus("CREATED");
        return ordersRepository.save(order);
    }

    // カート追加
    public CartItem addToCart(CartItem item) {
        return cartItemRepository.save(item);
    }

    // レビュー投稿
    public Review postReview(Review review) {
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());
        return reviewRepository.save(review);
    }

    // レビューいいね登録
    public ReviewLike likeReview(ReviewLike like) {
        return reviewLikeRepository.save(like);
    }

    // 注文一覧取得
    public List<Orders> getAllOrders() {
        return ordersRepository.findAll();
    }

    // ユーザー一覧取得
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
