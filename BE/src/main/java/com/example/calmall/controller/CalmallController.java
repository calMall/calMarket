package com.example.calmall.controller;

import com.example.calmall.entity.*;
import com.example.calmall.service.CalmallService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * フロントエンドと連携するためのREST APIコントローラー
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CalmallController {

    private final CalmallService calmallService;

    /**
     * 商品一覧を取得するエンドポイント
     * @return 全商品のリスト
     */
    @GetMapping("/products")
    public List<Product> getProducts() {
        return calmallService.getAllProducts();
    }

    /**
     * 商品を新規登録する（手動用）
     * @param product 登録する商品情報
     * @return 登録された商品情報
     */
    @PostMapping("/products")
    public Product addProduct(@RequestBody Product product) {
        return calmallService.addProduct(product);
    }

    /**
     * 楽天APIを使って商品を取得し、存在しない場合はDBに保存する
     * @param itemCode 楽天の商品コード（例: book:123456）
     * @return 楽天から取得した商品情報
     */
    @PostMapping("/rakuten/product/{itemCode}")
    public Product fetchRakutenProduct(@PathVariable String itemCode) {
        return calmallService.getOrInsertRakutenProduct(itemCode);
    }

    /**
     * ユーザー登録用エンドポイント
     * @param user 登録するユーザー情報
     * @return 登録されたユーザー
     */
    @PostMapping("/users")
    public User registerUser(@RequestBody User user) {
        return calmallService.registerUser(user);
    }

    /**
     * 注文を新規作成するエンドポイント
     * @param order 注文内容
     * @return 作成された注文情報
     */
    @PostMapping("/orders")
    public Orders createOrder(@RequestBody Orders order) {
        return calmallService.createOrder(order);
    }

    /**
     * カートに商品を追加するエンドポイント
     * @param item 追加するカートアイテム情報
     * @return 追加されたカートアイテム
     */
    @PostMapping("/cart")
    public CartItem addToCart(@RequestBody CartItem item) {
        return calmallService.addToCart(item);
    }

    /**
     * レビューを投稿するエンドポイント
     * @param review 投稿するレビュー情報
     * @return 投稿されたレビュー
     */
    @PostMapping("/reviews")
    public Review postReview(@RequestBody Review review) {
        return calmallService.postReview(review);
    }

    /**
     * レビューに「いいね」するエンドポイント
     * @param like いいね情報
     * @return 登録されたレビューいいね情報
     */
    @PostMapping("/review-likes")
    public ReviewLike likeReview(@RequestBody ReviewLike like) {
        return calmallService.likeReview(like);
    }

    /**
     * 全ての注文を取得するエンドポイント
     * @return 注文リスト
     */
    @GetMapping("/orders")
    public List<Orders> getOrders() {
        return calmallService.getAllOrders();
    }

    /**
     * 登録済みのユーザー一覧を取得するエンドポイント
     * @return ユーザーリスト
     */
    @GetMapping("/users")
    public List<User> getUsers() {
        return calmallService.getAllUsers();
    }
}
