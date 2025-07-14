package com.example.calmall.service;

import com.example.calmall.entity.*;
import com.example.calmall.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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

    /**
     * 全商品を取得（楽天APIの商品情報を含む）
     * @return 商品リスト
     */
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    /**
     * 商品を新規登録（楽天APIのitemCodeを使用）
     * - createdAtを現在時刻に設定
     * - inventoryが未設定の場合、5〜19でランダムに初期化
     *
     * @param product 登録する商品
     * @return 登録された商品
     */
    public Product addProduct(Product product) {
        product.setCreatedAt(LocalDateTime.now());

        // 在庫が指定されていない場合はランダムに設定
        if (product.getInventory() == null) {
            product.setInventory(ThreadLocalRandom.current().nextInt(5, 20));
        }

        return productRepository.save(product);
    }

    /**
     * ユーザーを新規登録
     * @param user 登録するユーザー情報
     * @return 登録されたユーザー
     */
    public User registerUser(User user) {
        return userRepository.save(user);
    }

    /**
     * 注文を作成し、ステータスと作成日を設定
     * @param order 注文情報
     * @return 登録された注文
     */
    public Orders createOrder(Orders order) {
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus("CREATED");
        return ordersRepository.save(order);
    }

    /**
     * カートに商品を追加
     * @param item カートアイテム情報
     * @return 追加されたカートアイテム
     */
    public CartItem addToCart(CartItem item) {
        return cartItemRepository.save(item);
    }

    /**
     * レビューを投稿（購入から1ヶ月以内のユーザーのみ可能）
     * - 条件を満たさない場合は例外をスロー
     *
     * @param review 投稿するレビュー情報
     * @return 登録されたレビュー
     */
    public Review postReview(Review review) {
        boolean eligible = ordersRepository.findAll().stream()
                .anyMatch(order ->
                        order.getUserId().equals(review.getUserId()) &&
                                order.getItemCode().equals(review.getItemCode()) &&
                                order.getCreatedAt().isAfter(LocalDateTime.now().minusMonths(1))
                );

        if (!eligible) {
            throw new IllegalArgumentException("レビューを投稿できるのは、購入後1ヶ月以内のユーザーのみです。");
        }

        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());
        return reviewRepository.save(review);
    }

    /**
     * レビューに「いいね」を追加
     * @param like いいね情報
     * @return 登録されたいいね
     */
    public ReviewLike likeReview(ReviewLike like) {
        return reviewLikeRepository.save(like);
    }

    /**
     * 全注文情報を取得
     * @return 注文リスト
     */
    public List<Orders> getAllOrders() {
        return ordersRepository.findAll();
    }

    /**
     * 全ユーザー情報を取得
     * @return ユーザーリスト
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}