package com.example.calmall.user.service;

import com.example.calmall.global.dto.ApiResponseDto;
import com.example.calmall.orders.entity.Orders;
import com.example.calmall.orders.repository.OrdersRepository;
import com.example.calmall.product.entity.Product;
import com.example.calmall.product.repository.ProductRepository;
import com.example.calmall.user.dto.*;
import com.example.calmall.user.entity.DeliveryAddress;
import com.example.calmall.user.entity.User;
import com.example.calmall.user.repository.DeliveryAddressRepository;
import com.example.calmall.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final OrdersRepository ordersRepository;
    private final ProductRepository productRepository;
    private final DeliveryAddressRepository deliveryAddressRepository;

    // ユーザー新規登録
    @Override
    public ResponseEntity<ApiResponseDto> register(UserRegisterRequestDto requestDto) {
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            return ResponseEntity.badRequest().body(new ApiResponseDto("既に登録されています"));
        }

        User newUser = new User();
        newUser.setUserId(generateUserId());
        newUser.setNickname(requestDto.getNickname());
        newUser.setEmail(requestDto.getEmail());
        newUser.setPassword(requestDto.getPassword());
        newUser.setBirth(requestDto.getBirth());
        newUser.setDeliveryAddresses(new ArrayList<>());
        newUser.setPoint(0);

        userRepository.save(newUser);
        return ResponseEntity.ok(new ApiResponseDto("success"));
    }

    // Email がすでに存在するか確認
    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // UUID形式のuserId生成
    private String generateUserId() {
        return "user_" + UUID.randomUUID().toString().substring(0, 8);
    }

    // ログイン処理
    @Override
    public User authenticate(UserLoginRequestDto requestDto) {
        Optional<User> userOpt = userRepository.findByEmail(requestDto.getEmail());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (requestDto.getPassword().equals(user.getPassword())) {
                return user;
            }
        }
        return null;
    }

    // ログアウト処理
    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDto> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false); // 既存セッションを取得（なければnull）
        if (session != null) {
            session.invalidate(); // セッションを無効化（ログアウト）
        }

        return ResponseEntity.ok(new ApiResponseDto("success"));
    }


    // ユーザー詳細取得
    @Override
    public ResponseEntity<UserDetailResponseDto> getUserDetail(String userId) {
        // ユーザーを取得
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが存在しません"));

        // 配送先住所一覧（String に変換して渡す）
        List<String> addressList = user.getDeliveryAddresses()
                .stream()
                .map(address -> address.getPostalCode() + " " + address.getAddress1() + " " + address.getAddress2())
                .toList();

        // TODO: 最新10件の注文とレビュー情報を詰める（今は空でOK）

        UserDetailResponseDto responseDto = UserDetailResponseDto.builder()
                .message("success")
                .point(user.getPoint())
                .deliveryAddresses(addressList)
                .orders(List.of())  // 仮
                .reviews(List.of()) // 仮
                .build();

        return ResponseEntity.ok(responseDto);
    }

    // 配送先住所追加（重複しない場合のみ保存）
    @Override
    public ResponseEntity<ApiResponseDto> addAddress(String userId, UserAddressRequestDto requestDto) {
        Optional<User> userOpt = userRepository.findByUserId(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiResponseDto("ユーザーが存在しません"));
        }

        User user = userOpt.get();

        // すでに同じ住所が存在するかチェック
        boolean exists = user.getDeliveryAddresses().stream().anyMatch(addr ->
                addr.getPostalCode().equals(requestDto.getPostalCode()) &&
                        addr.getAddress1().equals(requestDto.getAddress1()) &&
                        addr.getAddress2().equals(requestDto.getAddress2())
        );

        if (exists) {
            return ResponseEntity.badRequest().body(new ApiResponseDto("同じ住所が既に登録されています"));
        }

        // 住所追加処理
        DeliveryAddress address = new DeliveryAddress();
        address.setPostalCode(requestDto.getPostalCode());
        address.setAddress1(requestDto.getAddress1());
        address.setAddress2(requestDto.getAddress2());
        address.setUser(user);

        user.getDeliveryAddresses().add(address);
        deliveryAddressRepository.save(address);

        return ResponseEntity.ok(new ApiResponseDto("success"));
    }


    // 払い戻し処理（返金クーポンなし）
    @Override
    public ResponseEntity<RefundResponseDto> refund(RefundRequestDto requestDto) {
        Optional<Orders> orderOpt = ordersRepository.findById(requestDto.getOrderId());
        if (orderOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    RefundResponseDto.builder().message("fail").coupons(null).build());
        }

        Orders order = orderOpt.get();

        if ("REFUNDED".equals(order.getStatus())) {
            return ResponseEntity.badRequest().body(
                    RefundResponseDto.builder().message("fail").coupons(null).build());
        }

        Optional<User> userOpt = userRepository.findById(order.getUserId());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(
                    RefundResponseDto.builder().message("fail").coupons(null).build());
        }

        User user = userOpt.get();

        Optional<Product> productOpt = productRepository.findById(order.getProductItemCode());
        if (productOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    RefundResponseDto.builder().message("fail").coupons(null).build());
        }

        Product product = productOpt.get();

        // ポイント加算
        user.setPoint(user.getPoint() + product.getPrice());
        userRepository.save(user);

        // 注文ステータス更新
        order.setStatus("REFUNDED");
        ordersRepository.save(order);

        return ResponseEntity.ok(
                RefundResponseDto.builder()
                        .message("success")
                        .coupons(new ArrayList<>())
                        .build());
    }
}
