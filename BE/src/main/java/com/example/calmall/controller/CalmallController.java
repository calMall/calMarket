package com.example.calmall.controller;

import com.example.calmall.CartItem.CartItem;
import com.example.calmall.Orders.Orders;
import com.example.calmall.Product.Product;
import com.example.calmall.Review.Review;
import com.example.calmall.ReviewLike.ReviewLike;
import com.example.calmall.User.User;
import com.example.calmall.service.CalmallService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * APIエンドポイントを定義するコントローラー
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CalmallController {

    private final CalmallService calmallService;

    // 商品一覧取得
    @GetMapping("/products")
    public List<Product> getProducts() {
        return calmallService.getAllProducts();
    }

    // 商品登録
    @PostMapping("/products")
    public Product addProduct(@RequestBody Product product) {
        return calmallService.addProduct(product);
    }

    // ユーザー登録
    @PostMapping("/users")
    public User registerUser(@RequestBody User user) {
        return calmallService.registerUser(user);
    }

    // 注文作成
    @PostMapping("/orders")
    public Orders createOrder(@RequestBody Orders order) {
        return calmallService.createOrder(order);
    }

    // カートに追加
    @PostMapping("/cart")
    public CartItem addToCart(@RequestBody CartItem item) {
        return calmallService.addToCart(item);
    }

    // レビュー投稿
    @PostMapping("/reviews")
    public Review postReview(@RequestBody Review review) {
        return calmallService.postReview(review);
    }

    // レビューいいね
    @PostMapping("/review-likes")
    public ReviewLike likeReview(@RequestBody ReviewLike like) {
        return calmallService.likeReview(like);
    }

    // 注文一覧取得
    @GetMapping("/orders")
    public List<Orders> getOrders() {
        return calmallService.getAllOrders();
    }

    // ユーザー一覧取得
    @GetMapping("/users")
    public List<User> getUsers() {
        return calmallService.getAllUsers();
    }
}
