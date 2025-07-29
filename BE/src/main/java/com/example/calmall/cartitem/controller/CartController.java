package com.example.calmall.cartitem.controller;

import com.example.calmall.cartitem.dto.CartAddRequestDto;
import com.example.calmall.cartitem.dto.CartListResponseDto;
import com.example.calmall.cartitem.entity.CartItem;
import com.example.calmall.cartitem.service.CartItemService;
import com.example.calmall.global.dto.ApiResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Optional;

/**
 * カート機能に関するAPIエンドポイントを提供するコントローラー。
 */
@RestController // RESTful APIコントローラーであることを示す
@RequestMapping("/api/cart") // ベースパスを設定
@RequiredArgsConstructor // finalフィールドのコンストラクタを自動生成
@Slf4j // ロギング用のアノテーション
public class CartController { 
    

    private final CartItemService cartItemService;
    private String getUserIdFromSession(HttpServletRequest request) {
        //ユーザセッション確認
        String userId = (String) request.getSession().getAttribute("userId");
        if (userId == null) {
            log.warn("セッションにユーザーIDが見つかりませんでした。");
            // ログインが必要な場合はnullを返す
            return null;
        }
        return userId;

    }

    /**
     * カートに商品を追加、または既存商品の数量を調整します。
     * 数量が0になった場合は商品を削除します。
     *
     * @param requestDto カート追加/更新リクエストDTO
     * @param request HttpServletRequest (ユーザーID取得のため)
     * @return 成功または失敗を示すApiResponseDto
     */
    @PostMapping
    public ResponseEntity<ApiResponseDto> addOrUpdateCartItem(@RequestBody CartAddRequestDto requestDto,
                                                              HttpServletRequest request) {
        String userId = getUserIdFromSession(request);
        if (userId == null) {
            log.warn("ログインが必要です。");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDto("ログインが必要です"));
        }

        try {
            Optional<CartItem> result = cartItemService.addOrUpdateCartItem(userId, requestDto);
            if (result.isPresent()) {
                log.info("カートアイテムが追加または更新されました。userId={}, itemCode={}, quantity={}", userId, requestDto.getItemCode(), requestDto.getQuantity());
                return ResponseEntity.ok(new ApiResponseDto("success"));
            } else {
                // 数量が0になり削除された場合も成功とみなす
                log.info("カートアイテムが削除されました (数量0)。userId={}, itemCode={}", userId, requestDto.getItemCode());
                return ResponseEntity.ok(new ApiResponseDto("success"));
            }
        } catch (IllegalArgumentException e) {
            log.error("カート追加/更新リクエストの引数エラー: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDto("fail: " + e.getMessage()));
        } catch (Exception e) {
            log.error("カート操作中に予期せぬエラーが発生しました。userId={}, requestDto={}: {}", userId, requestDto, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDto("fail: カートの操作に失敗しました"));
        }
    }

    /**
     * ログイン中のユーザーのカート内容を取得します。
     *
     * @param request HttpServletRequest (ユーザーID取得のため)
     * @return カートアイテムのリストを含むCartListResponseDto
     */
    @GetMapping
    public ResponseEntity<CartListResponseDto> getCartItems(HttpServletRequest request) {
        String userId = getUserIdFromSession(request);
        if (userId == null) {
            log.warn("ログインが必要です。");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    CartListResponseDto.builder()
                            .message("fail")
                            .cartItems(Collections.emptyList()) // 空のリストを返す
                            .build()
            );
        }

        try {
            CartListResponseDto response = cartItemService.getCartItemsForUser(userId);
            log.info("ユーザーのカート内容を取得しました。userId={}, itemsCount={}", userId, response.getCartItems().size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("カート内容取得中に予期せぬエラーが発生しました。userId={}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CartListResponseDto.builder()
                            .message("fail")
                            .cartItems(Collections.emptyList()) // エラー時は空のリストを返す
                            .build()
            );
        }
    }

    /**
     * カートから特定のアイテムを削除します。
     *
     * @param cartItemId 削除するカートアイテムのID
     * @param request HttpServletRequest (ユーザーID取得のため)
     * @return 成功または失敗を示すApiResponseDto
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto> removeCartItem(@PathVariable("id") Long cartItemId,
                                                         HttpServletRequest request) {
        String userId = getUserIdFromSession(request);
        if (userId == null) {
            log.warn("ログインが必要です。");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDto("ログインが必要です"));
        }

        try {
            boolean removed = cartItemService.removeCartItemById(userId, cartItemId);
            if (removed) {
                log.info("カートアイテムが削除されました。userId={}, cartItemId={}", userId, cartItemId);
                return ResponseEntity.ok(new ApiResponseDto("success"));
            } else {
                log.warn("カートアイテムの削除に失敗しました。指定された商品が見つからないか、アクセス権がありません。userId={}, cartItemId={}", userId, cartItemId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDto("fail: 指定された商品がカートに見つからないか、アクセス権がありません"));
            }
        } catch (Exception e) {
            log.error("カートアイテム削除中に予期せぬエラーが発生しました。userId={}, cartItemId={}: {}", userId, cartItemId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDto("fail: カート商品の削除に失敗しました"));
        }
    }

    /**
     * ログイン中のユーザーのカート内のすべてのアイテムを一括削除します。
     *
     * @param request HttpServletRequest (ユーザーID取得のため)
     * @return 成功または失敗を示すApiResponseDto
     */
    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponseDto> clearUserCart(HttpServletRequest request) {
        String userId = getUserIdFromSession(request);
        if (userId == null) {
            log.warn("ログインが必要です。");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDto("ログインが必要です"));
        }

        try {
            cartItemService.clearCart(userId);
            log.info("ユーザーのカートがクリアされました。userId={}", userId);
            return ResponseEntity.ok(new ApiResponseDto("success"));
        } catch (Exception e) {
            log.error("カートクリア中に予期せぬエラーが発生しました。userId={}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDto("fail: カートのクリアに失敗しました"));
        }
    }
}
