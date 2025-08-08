package com.example.calmall.user.service;

import com.example.calmall.global.dto.ApiResponseDto;
import com.example.calmall.orders.entity.Orders;
import com.example.calmall.orders.repository.OrdersRepository;
import com.example.calmall.product.entity.Product;
import com.example.calmall.product.repository.ProductRepository;
import com.example.calmall.review.entity.Review;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ユーザー関連のビジネスロジックを提供するサービス実装クラス
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    // 各種リポジトリを依存注入
    private final UserRepository userRepository;
    private final OrdersRepository ordersRepository;
    private final ProductRepository productRepository;
    private final DeliveryAddressRepository deliveryAddressRepository;
    private final DeliveryAddressRepository addressRepository;

    /**
     * ユーザー登録処理
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
        newUser.setDeliveryAddresses(new ArrayList<>());
        newUser.setPoint(0);

        userRepository.save(newUser);
        return ResponseEntity.ok(new ApiResponseDto("success"));
    }

    /**
     * Email重複チェック
     */
    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * UUID形式のuserId生成ヘルパー
     */
    private String generateUserId() {
        return "user_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 認証処理（ログイン）
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
     * ログアウト処理（セッション無効化）
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
     * - 所持ポイント、配送先住所、注文履歴、レビュー履歴を返却
     */
    @Override
    public ResponseEntity<UserDetailResponseDto> getUserDetail(String userId) {
        // ユーザー取得（存在しなければ例外）
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが存在しません"));

        // 配送先住所リストを文字列に変換（郵便番号 + 住所1 + 住所2）
        List<String> addressList = Optional.ofNullable(user.getDeliveryAddresses())
                .orElse(Collections.emptyList())
                .stream()
                .map(addr -> String.format("%s %s %s",
                                Optional.ofNullable(addr.getPostalCode()).orElse(""),
                                Optional.ofNullable(addr.getAddress1()).orElse(""),
                                Optional.ofNullable(addr.getAddress2()).orElse(""))
                        .trim())
                .collect(Collectors.toList());

        // ★追加：構造化住所リスト（postalCode / address1 / address2をそのまま保持）
        List<UserDetailResponseDto.AddressDetail> addressDetails =
                Optional.ofNullable(user.getDeliveryAddresses())
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(addr -> UserDetailResponseDto.AddressDetail.builder()
                                .postalCode(Optional.ofNullable(addr.getPostalCode()).orElse(""))
                                .address1(Optional.ofNullable(addr.getAddress1()).orElse(""))
                                .address2(Optional.ofNullable(addr.getAddress2()).orElse(""))
                                .build())
                        .collect(Collectors.toList());

//        TODO:
//        // 注文履歴（最新10件）を取得し、OrderSummary に変換
//        List<UserDetailResponseDto.OrderSummary> orderSummaries = ordersRepository.findTop10ByUserOrderByCreatedAtDesc(user)
//                .stream()
//                .map(order -> {
//                    Product product = order.getProduct();
//                    String imageUrl = (product != null && product.getImages() != null && !product.getImages().isEmpty())
//                            ? product.getImages().get(0) : null;
//                    return UserDetailResponseDto.OrderSummary.builder()
//                            .id(order.getId())
//                            .imageUrl(imageUrl)
//                            .build();
//                })
//                .collect(Collectors.toList());

        // レビュー履歴（最新10件）を取得し、ReviewSummary に変換
        List<UserDetailResponseDto.ReviewSummary> reviewSummaries = new ArrayList<>();

        if (user.getReviews() != null) {
            reviewSummaries = user.getReviews().stream()
                    .sorted(Comparator.comparing(Review::getCreatedAt).reversed())
                    .limit(10)
                    .map(review -> UserDetailResponseDto.ReviewSummary.builder()
                            .id(review.getReviewId())
                            .title(review.getTitle())
                            .createdAt(review.getCreatedAt())
                            .score(review.getRating())
                            .content(review.getComment())
                            .deliveryAddresses(addressList)
                            .build())
                    .collect(Collectors.toList());
        }

        // DTOを構築して返却
        UserDetailResponseDto responseDto = UserDetailResponseDto.builder()
                .message("success")
                .point(Optional.ofNullable(user.getPoint()).orElse(0))
                .deliveryAddresses(addressList)
                .deliveryAddressDetails(addressDetails)
//                .orders(orderSummaries)   注文履歴
                .reviews(reviewSummaries)
                .build();

        return ResponseEntity.ok(responseDto);
    }


    /**
     * 配送先住所の追加（最大3件まで登録可能）
     */
    @Transactional
    @Override
    public ResponseEntity<ApiResponseDto> addAddress(String userId, UserAddressRequestDto requestDto) {
        // ユーザーを取得
        User user = userRepository.findByUserId(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(new ApiResponseDto("ユーザーが存在しません"));
        }

        // 配送先住所リストがnullの場合新しく作成
        if (user.getDeliveryAddresses() == null) {
            user.setDeliveryAddresses(new ArrayList<>());
        }

        // 住所が3件以上登録不可
        if (user.getDeliveryAddresses().size() >= 3) {
            return ResponseEntity.badRequest().body(new ApiResponseDto("住所は最大3件までしか登録できません"));
        }

        // 入力値をトリムして比較用に整形
        String pc = requestDto.getPostalCode().trim();
        String a1 = requestDto.getAddress1().trim();
        String a2 = requestDto.getAddress2().trim();

        // 同一の住所がすでに登録されているか確認
        boolean exists = user.getDeliveryAddresses().stream().anyMatch(addr ->
                pc.equals(addr.getPostalCode()) &&
                        a1.equals(addr.getAddress1()) &&
                        a2.equals(addr.getAddress2())
        );
        if (exists) {
            return ResponseEntity.badRequest().body(new ApiResponseDto("同じ住所が既に登録されています"));
        }

        // 新しい住所を作成してリストに追加
        DeliveryAddress address = new DeliveryAddress();
        address.setPostalCode(pc);
        address.setAddress1(a1);
        address.setAddress2(a2);
        address.setUser(user);
        user.getDeliveryAddresses().add(address);

        // ユーザー情報を保存
        userRepository.save(user);
        return ResponseEntity.ok(new ApiResponseDto("success"));
    }

    /**
     * 指定された配送先住所を削除する処理
     */
    @Override
    @Transactional
    public ResponseEntity<ApiResponseDto> deleteAddress(String userId, UserAddressRequestDto requestDto) {
        // ユーザーをDBから取得
        User user = userRepository.findByUserId(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(new ApiResponseDto("ユーザーが存在しません"));
        }

        // ユーザーの住所リストが空またはnullの場合
        if (user.getDeliveryAddresses() == null || user.getDeliveryAddresses().isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiResponseDto("削除できる住所が存在しません"));
        }

        // 入力された住所情報をトリム
        String postalCode = requestDto.getPostalCode().trim();
        String address1 = requestDto.getAddress1().trim();
        String address2 = requestDto.getAddress2().trim();

        // 該当する住所をリストから探す
        Optional<DeliveryAddress> targetOpt = user.getDeliveryAddresses().stream()
                .filter(addr -> postalCode.equals(addr.getPostalCode())
                        && address1.equals(addr.getAddress1())
                        && address2.equals(addr.getAddress2()))
                .findFirst();

        // 該当住所が存在しない場合
        if (targetOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiResponseDto("該当する住所が見つかりません"));
        }

        // ユーザーリストから削除し、DBからも削除
        DeliveryAddress target = targetOpt.get();
        user.getDeliveryAddresses().remove(target);
        addressRepository.delete(target);

        return ResponseEntity.ok(new ApiResponseDto("success"));
    }


    //    TODO:
//    /**
//     * 注文の払い戻し処理
//     */
//    @Override
    public ResponseEntity<RefundResponseDto> refund(RefundRequestDto requestDto) {
//        Optional<Orders> orderOpt = ordersRepository.findById(requestDto.getOrderId());
//        if (orderOpt.isEmpty()) {
//            return ResponseEntity.badRequest().body(
//                    RefundResponseDto.builder().message("fail").coupons(null).build());
//        }
//
//        Orders order = orderOpt.get();
//
//        if ("REFUNDED".equals(order.getStatus())) {
//            return ResponseEntity.badRequest().body(
//                    RefundResponseDto.builder().message("fail").coupons(null).build());
//        }
//
//        User user = order.getUser();
//        Product product = order.getProduct();
//
//        if (user == null || product == null) {
//            return ResponseEntity.badRequest().body(
//                    RefundResponseDto.builder().message("fail").coupons(null).build());
//        }
//
//        user.setPoint(user.getPoint() + product.getPrice());
//        userRepository.save(user);
//
//        order.setStatus("REFUNDED");
//        ordersRepository.save(order);
//
//        return ResponseEntity.ok(
//                RefundResponseDto.builder()
//                        .message("success")
//                        .coupons(new ArrayList<>())
//                        .build());
        // TODO: 後で払い戻しロジックを実装する
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(RefundResponseDto.builder()
                        .message("未実装")
                        .coupons(null)
                        .build());
    }
}
