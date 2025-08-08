package com.example.calmall.orders.service;

<<<<<<< HEAD
import com.example.calmall.orders.dto.FinalOrderRequestDto;
import com.example.calmall.orders.dto.OrderCheckResponseDto;
=======
import com.example.calmall.orders.dto.OrderCheckResponseDto;
import com.example.calmall.orders.dto.OrderRequestDto;
>>>>>>> 38d7c7ae6928b4662b57f8a7985d1fec4ef8c16c
import com.example.calmall.orders.entity.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface OrderService {
    // 注文前チェックを行う新しいメソッド
    OrderCheckResponseDto checkOrder(List<String> itemCodes, String userId);

    // 注文を最終的に確定する新しいメソッド
    void finalizeOrder(FinalOrderRequestDto requestDto, String userId);
    
    // 注文状況
    void updateOrderStatus();
    
    // キャンセル
    boolean canCancel(Long orderId, String userId);
    void cancelOrder(Long orderId, String userId);
    
    // 払い戻し
    boolean canRefund(Long orderId, String userId);
    void refundOrder(Long orderId, String userId);
    
    // 注文リスト
    List<Orders> findOrdersByUserId(String userId);
    
    // ページネーション
    Page<Orders> findOrdersByUserId(String userId, Pageable pageable);
    
    // 注文詳細
    Optional<Orders> getOrderByIdAndUserId(Long orderId, String userId);
<<<<<<< HEAD
}
=======
    //仮注文
    OrderCheckResponseDto checkOrder(OrderRequestDto requestDto, String userId);

}
>>>>>>> 38d7c7ae6928b4662b57f8a7985d1fec4ef8c16c
