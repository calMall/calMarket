package com.example.calmall.user.service;

import com.example.calmall.global.dto.ApiResponseDto;
import com.example.calmall.orders.entity.Orders;
import com.example.calmall.orders.entity.OrderItems;
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
    private final DeliveryAddressRepository addressRepository;


    /** 文字列の正規化: null→""、trim */
    private String normalize(String s) { return (s == null) ? "" : s.trim(); }

    //  UUID形式のuserId生成ヘルパー
    private String generateUserId() {
        return "user_" + UUID.randomUUID().toString().substring(0, 8);
    }



    // ユーザー登録処理
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


    //  Email重複チェック
    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }


     // 認証処理（ログイン）
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


    // ログアウト処理（セッション無効化）
    @Override
    public ResponseEntity<ApiResponseDto> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(new ApiResponseDto("success"));
    }

    //ユーザー詳細情報の取得
    @Override
    public ResponseEntity<UserDetailResponseDto> getUserDetail(String userId) {
        // ユーザー取得（存在しなければ例外）
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが存在しません"));

        // 配送先住所（文字列リスト）
        List<String> addressList = Optional.ofNullable(user.getDeliveryAddresses())
                .orElse(Collections.emptyList())
                .stream()
                .map(addr -> String.format("%s %s %s",
                                Optional.ofNullable(addr.getPostalCode()).orElse(""),
                                Optional.ofNullable(addr.getAddress1()).orElse(""),
                                Optional.ofNullable(addr.getAddress2()).orElse(""))
                        .trim())
                .collect(Collectors.toList());

        // 構造化住所
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

        // 注文履歴（最新10件）id + 画像URL
        List<UserDetailResponseDto.OrderSummary> orderSummaries =
                Optional.ofNullable(ordersRepository.findTop10ByUserOrderByCreatedAtDesc(user))
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(order -> {
                            String imageUrl = null;

                            // 最初の明細（存在すれば）
                            List<OrderItems> items = order.getOrderItems();
                            if (items != null && !items.isEmpty()) {
                                OrderItems first = items.get(0);

                                String urls = first.getImageListUrls();
                                if (urls != null && !urls.isBlank()) {
                                    String[] parts = urls.split(",");
                                    if (parts.length > 0) {
                                        imageUrl = parts[0].trim();
                                    }
                                }

                                if (imageUrl == null || imageUrl.isBlank()) {
                                    Product product = first.getProduct();
                                    if (product != null && product.getImages() != null && !product.getImages().isEmpty()) {
                                        imageUrl = product.getImages().get(0);
                                    }
                                }
                            }

                            return UserDetailResponseDto.OrderSummary.builder()
                                    .id(order.getId())
                                    .imageUrl(imageUrl)
                                    .build();
                        })
                        .collect(Collectors.toList());

        // レビュー履歴（最新10件）
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
                .orders(orderSummaries)
                .reviews(reviewSummaries)
                .build();

        return ResponseEntity.ok(responseDto);
    }

    // 配送先住所の追加（最大3件まで）
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

        // 入力値を正規化（null→""、trim）
        String pc = normalize(requestDto.getPostalCode());
        String a1 = normalize(requestDto.getAddress1());
        String a2 = normalize(requestDto.getAddress2());

        // 同一の住所がすでに登録されているか確認（address2無視）
        boolean exists = user.getDeliveryAddresses().stream().anyMatch(addr ->
                pc.equals(normalize(addr.getPostalCode())) &&
                        a1.equals(normalize(addr.getAddress1()))
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


    // 指定された配送先住所を削除する処理
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

        // 入力値を正規化
        String postalCode = normalize(requestDto.getPostalCode());
        String address1 = normalize(requestDto.getAddress1());

        // 該当住所リスト探す（postalCode + address1の一致）
        Optional<DeliveryAddress> targetOpt = user.getDeliveryAddresses().stream()
                .filter(addr -> postalCode.equals(normalize(addr.getPostalCode()))
                        && address1.equals(normalize(addr.getAddress1())))
                .findFirst();

        // 該当住所が存在しない場合
        if (targetOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiResponseDto("該当住所が見つかりません"));
        }

        // ユーザーリストから削除し、DBからも削除
        DeliveryAddress target = targetOpt.get();
        user.getDeliveryAddresses().remove(target);
        addressRepository.delete(target);

        return ResponseEntity.ok(new ApiResponseDto("success"));
    }
}
