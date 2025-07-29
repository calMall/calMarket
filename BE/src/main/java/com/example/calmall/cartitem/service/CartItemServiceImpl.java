package com.example.calmall.cartitem.service;

import com.example.calmall.cartitem.dto.CartAddRequestDto;
import com.example.calmall.cartitem.dto.CartListResponseDto;
import com.example.calmall.cartitem.entity.CartItem;
import com.example.calmall.cartitem.repository.CartItemRepository;
import com.example.calmall.product.dto.ProductDetailResponseDto; // ProductDetailResponseDto をインポート
import com.example.calmall.product.service.ProductService; // ProductService をインポート
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // ロギングを追加
import org.springframework.http.ResponseEntity; // ResponseEntity を使用するため

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartItemServiceImpl implements CartItemService {

    private final CartItemRepository cartItemRepository;
    private final ProductService productService; // ProductService を注入

    /**
     * カートアイテムの追加または数量更新、および数量が0以下の場合の削除。
     *
     * @param userId 
     * @param requestDto 
     * @return 
     * @throws IllegalArgumentException
     */
    @Override
    @Transactional // トランザクション管理
    public Optional<CartItem> addOrUpdateCartItem(String userId, CartAddRequestDto requestDto) {
        // バリデーション (Controllerで@Validを使わないため、Serviceで手動チェック)
        if (requestDto.getQuantity() < 0) {
            log.warn("数量は0以上である必要があります。userId={}, itemCode={}, quantity={}", userId, requestDto.getItemCode(), requestDto.getQuantity());
            throw new IllegalArgumentException("数量は0以上である必要があります。");
        }
        if (requestDto.getItemCode() == null || requestDto.getItemCode().trim().isEmpty()) {
            log.warn("商品コードは必須です。userId={}", userId);
            throw new IllegalArgumentException("商品コードは必須です。");
        }

        // 追加しようとしている商品が、既にそのユーザーのカートに存在するかどうかを確認
        Optional<CartItem> existingCartItem = cartItemRepository.findAll().stream()
                .filter(ci -> ci.getUserId().equals(userId) &&
                               ci.getItemCode().equals(requestDto.getItemCode()) &&
                               ci.getOption() == null)
                .findFirst();

        if (existingCartItem.isPresent()) {
            // 既存のアイテムがある場合、数量を更新
            CartItem cartItem = existingCartItem.get();
            cartItem.setQuantity(requestDto.getQuantity());

            if (cartItem.getQuantity() <= 0) {
                // 数量が0以下になった場合、カートアイテムを削除
                cartItemRepository.delete(cartItem);
                log.info("カートアイテムを削除しました。userId={}, itemCode={}", userId, requestDto.getItemCode());
                return Optional.empty(); // 削除されたことを示す
            } else {
                // 数量が正の場合、更新を保存
                CartItem savedCartItem = cartItemRepository.save(cartItem);
                log.info("カートアイテムの数量を更新しました。userId={}, itemCode={}, newQuantity={}", userId, requestDto.getItemCode(), savedCartItem.getQuantity());
                return Optional.of(savedCartItem);
            }
        } else {
            // 新しいアイテムの場合
            if (requestDto.getQuantity() <= 0) {
                // 新規追加で数量が0以下の場合は何もしない
                log.warn("新規追加で数量が0以下のため、カートアイテムは追加されませんでした。userId={}, itemCode={}", userId, requestDto.getItemCode());
                return Optional.empty();
            }
            // 新しいCartItemを作成し、保存
            CartItem cartItem = new CartItem();
            cartItem.setUserId(userId);
            cartItem.setItemCode(requestDto.getItemCode());
            cartItem.setQuantity(requestDto.getQuantity());
            cartItem.setOption(null);
            CartItem savedCartItem = cartItemRepository.save(cartItem);
            log.info("新しいカートアイテムを追加しました。userId={}, itemCode={}, quantity={}", userId, savedCartItem.getItemCode(), savedCartItem.getQuantity());
            return Optional.of(savedCartItem);
        }
    }

    /**
     * 指定されたユーザーのカートアイテムリストを取得
     * 各カートアイテムには、ProductServiceから取得した商品詳細情報が含まれる
     *
     * @param userId ユーザーID
     * @return カートアイテムのリストを含むCartListResponseDto
     */
    @Override
    public CartListResponseDto getCartItemsForUser(String userId) {
        //データベースから検索
        List<CartItem> entityCartItems = cartItemRepository.findAll().stream()
                .filter(ci -> ci.getUserId().equals(userId))
                .collect(Collectors.toList());

        // CartItemエンティティとProductServiceからの商品詳細情報を組み合わせてDTOリストを作成
        List<CartListResponseDto.CartItemDto> dtoList = entityCartItems.stream()
                .map(entity -> {
                    // ProductServiceを使用して商品詳細を取得
                    ResponseEntity<ProductDetailResponseDto> productDetailResponse = productService.getProductDetail(entity.getItemCode());
                    ProductDetailResponseDto.ProductDto productDto = null;

                    // 商品詳細が正常に取得できたかチェック
                    if (productDetailResponse.getStatusCode().is2xxSuccessful() && productDetailResponse.getBody() != null && productDetailResponse.getBody().getProduct() != null) {
                        productDto = productDetailResponse.getBody().getProduct();
                        log.debug("商品詳細取得成功: itemCode={}", entity.getItemCode());
                    } else {
                        // 商品詳細が取得できなかった場合のフォールバック
                        log.warn("商品詳細が取得できませんでした。itemCode={}, HTTP Status={}", entity.getItemCode(), productDetailResponse.getStatusCode());
                        productDto = ProductDetailResponseDto.ProductDto.builder()
                                .itemCode(entity.getItemCode())
                                .itemName("不明な商品") 
                                .price(0) 
                                .imageUrls(List.of("https://placehold.co/100x100/CCCCCC/000000?text=NoImage"))
                                .build();
                    }

                    // CartItemDtoをビルドして返す (CartItemDtoにはidフィールドがないため、idはマッピングしない)
                    return CartListResponseDto.CartItemDto.builder()
                            .itemCode(entity.getItemCode())
                            .itemName(productDto.getItemName())
                            .price(productDto.getPrice())
                            .quantity(entity.getQuantity())
                            .imageUrls(productDto.getImageUrls())
                            .option(entity.getOption())
                            .build();
                })
                .collect(Collectors.toList());

        log.info("ユーザーのカートアイテムリストを取得しました。userId={}, count={}", userId, dtoList.size());
        return CartListResponseDto.builder()
                .message("success")
                .cartItems(dtoList)
                .build();
    }

    /**
     * 指定されたカートアイテムIDを持つ商品をユーザーのカートから削除
     *
     * @param userId ユーザーID
     * @param cartItemId 削除するカートアイテムのID
     * @return 削除が成功した場合はtrue、見つからないかアクセス権がない場合はfalse
     */
    @Override
    @Transactional // トランザクション管理
    public boolean removeCartItemById(String userId, Long cartItemId) {
        Optional<CartItem> cartItemOptional = cartItemRepository.findById(cartItemId);

        if (cartItemOptional.isPresent()) {
            CartItem cartItem = cartItemOptional.get();
            // ユーザーIDが一致するか確認 (セキュリティチェック)
            if (cartItem.getUserId().equals(userId)) {
                cartItemRepository.delete(cartItem);
                log.info("カートアイテムが削除されました。userId={}, cartItemId={}", userId, cartItemId);
                return true;
            } else {
                log.warn("指定されたIDのカートアイテムは他のユーザーのものです。削除を拒否しました。userId={}, cartItemId={}", userId, cartItemId);
            }
        } else {
            log.warn("指定されたIDのカートアイテムが見つかりませんでした。cartItemId={}", cartItemId);
        }
        return false;
    }

    /**
     * 指定されたユーザーのカート内のすべてのアイテムを削除します。
     *
     * @param userId ユーザーID
     */
    @Override
    @Transactional // トランザクション管理
    public void clearCart(String userId) {
        List<CartItem> userCartItems = cartItemRepository.findAll().stream()
                .filter(ci -> ci.getUserId().equals(userId))
                .collect(Collectors.toList());
        cartItemRepository.deleteAll(userCartItems); // 取得したリストを一括削除
        log.info("ユーザーのカートがクリアされました。userId={}", userId);
    }

    /**
     * 指定されたユーザーのカート内のアイテム数を取得します。
     *
     * @param userId ユーザーID
     * @return カートアイテムの総数
     */
    @Override
    public int getCartItemCount(String userId) {
        int count = (int) cartItemRepository.findAll().stream()
                .filter(ci -> ci.getUserId().equals(userId))
                .count();
        log.info("ユーザーのカートアイテム数を取得しました。userId={}, count={}", userId, count);
        return count;
    }
}
