package com.example.calmall.global.service;

import com.example.calmall.cartitem.entity.CartItem;
import com.example.calmall.orders.entity.Orders;
import com.example.calmall.product.entity.Product;
import com.example.calmall.review.entity.Review;
import com.example.calmall.reviewLike.entity.ReviewLike;
import com.example.calmall.user.entity.User;

import java.util.List;

public interface CalmallService {
    Product getOrInsertRakutenProduct(String itemCode);
    List<Product> getAllProducts();
    Product addProduct(Product product);
    User registerUser(User user);
    List<User> getAllUsers();
    Orders createOrder(Orders order);
    List<Orders> getAllOrders();
    CartItem addToCart(CartItem item);
    Review postReview(Review review);
    ReviewLike likeReview(ReviewLike like);
}
