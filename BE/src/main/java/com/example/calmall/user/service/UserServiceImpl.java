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
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ユーザー関連のビジネスロジックを実装するサービスクラス
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    // 各種リポジトリを依存注入
    private final UserRepository userRepository;
    private final OrdersRepository ordersRepository;
    private final ProductRepository productRepository;
    private final DeliveryAddressRepository deliveryAddressRepository;

    /**
     * ユーザー新規登録処理
     * - email が重複していないか確認
     * - UUID形式のuserIdを生成し、初期情報を保存
     */
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
        newUser.setDeliveryAddresses(new ArrayList<>()); // 空の住所リストで初期化
        newUser.setPoint(0); // 初期ポイント0

        userRepository.save(newUser);
        return ResponseEntity.ok(new ApiResponseDto("success"));
    }

    /**
     * 指定された email が既に登録されているかチェック
     */
    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * UUID形式のuserIdを生成するヘルパーメソッド
     */
    private String generateUserId() {
        return "user_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * ログイン処理
     * - emailとパスワードの一致を確認
     * - 成功時はUserオブジェクトを返却
     */
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

    /**
     * ログアウト処理
     * - セッションを無効化
     */
    @Override
    public ResponseEntity<ApiResponseDto> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(new ApiResponseDto("success"));
    }

    /**
     * ユーザー詳細情報の取得
     * - 指定されたuserIdに基づいて情報を取得
     * - 配送先住所、所持ポイントを返却
     */
    @Override
    public ResponseEntity<UserDetailResponseDto> getUserDetail(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが存在しません"));

        // 住所リストを「郵便番号 住所1 住所2」の形式で文字列化
        List<String> addressList = Optional.ofNullable(user.getDeliveryAddresses())
                .orElse(Collections.emptyList())
                .stream()
                .map(addr -> String.format("%s %s %s",
                                Optional.ofNullable(addr.getPostalCode()).orElse(""),
                                Optional.ofNullable(addr.getAddress1()).orElse(""),
                                Optional.ofNullable(addr.getAddress2()).orElse(""))
                        .trim())
                .collect(Collectors.toList());

        // 注文履歴やレビュー履歴は現時点では空
        UserDetailResponseDto responseDto = UserDetailResponseDto.builder()
                .message("success")
                .point(Optional.ofNullable(user.getPoint()).orElse(0))
                .deliveryAddresses(addressList)
                .orders(Collections.emptyList())
                .reviews(Collections.emptyList())
                .build();

        return ResponseEntity.ok(responseDto);
    }

    /**
     * ユーザーの配送先住所を追加
     * - 同一内容の住所が既に登録されている場合は登録しない
     * - 親(User)側から cascade 保存
     */
    @Transactional
    @Override
    public ResponseEntity<ApiResponseDto> addAddress(String userId, UserAddressRequestDto requestDto) {

        // 1) ユーザー取得
        User user = userRepository.findByUserId(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(new ApiResponseDto("ユーザーが存在しません"));
        }

        // 2) リスト null 防止
        if (user.getDeliveryAddresses() == null) {
            user.setDeliveryAddresses(new ArrayList<>());
        }

        // 3) 入力値をトリム（重複判定の精度UP）
        String pc = requestDto.getPostalCode().trim();
        String a1 = requestDto.getAddress1().trim();
        String a2 = requestDto.getAddress2().trim();

        // 4) 重複チェック
        boolean exists = user.getDeliveryAddresses().stream().anyMatch(addr ->
                pc.equals(addr.getPostalCode()) &&
                        a1.equals(addr.getAddress1()) &&
                        a2.equals(addr.getAddress2())
        );
        if (exists) {
            return ResponseEntity.badRequest().body(new ApiResponseDto("同じ住所が既に登録されています"));
        }

        // 5) 子エンティティ生成 & 双方向リンク
        DeliveryAddress address = new DeliveryAddress();
        address.setPostalCode(pc);
        address.setAddress1(a1);
        address.setAddress2(a2);
        address.setUser(user);                    // Owning side (FK)
        user.getDeliveryAddresses().add(address); // Inverse side

        // 6) 親を保存（cascade=ALLなので子も保存）
        userRepository.save(user);                // saveAndFlush(user) でも可

        return ResponseEntity.ok(new ApiResponseDto("success"));
    }

    /**
     * 注文払い戻し処理
     * - 注文ステータスを「REFUNDED」に変更し、商品金額分のポイントを返却
     */
    @Override
    public ResponseEntity<RefundResponseDto> refund(RefundRequestDto requestDto) {
        Optional<Orders> orderOpt = ordersRepository.findById(requestDto.getOrderId());
        if (orderOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    RefundResponseDto.builder().message("fail").coupons(null).build());
        }

        Orders order = orderOpt.get();

        // 既に返金済みかチェック
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
