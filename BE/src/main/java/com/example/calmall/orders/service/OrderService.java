package com.example.calmall.orders.service;

import com.example.calmall.orders.dto.OrderCheckResponseDto;
import com.example.calmall.orders.dto.OrderRequestDto;
import com.example.calmall.orders.entity.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface OrderService {
    //注文作成
    Orders createOrder(OrderRequestDto requestDto, String userId); 
    //注文状況
    void updateOrderStatus();
    //キャンセル
    boolean canCancel(Long orderId, String userId);
    void cancelOrder(Long orderId, String userId);
    //払い戻し
    boolean canRefund(Long orderId, String userId);
    void refundOrder(Long orderId, String userId);
    //注文リスト
    List<Orders> findOrdersByUserId(String userId);
    //ページネーション
    Page<Orders> findOrdersByUserId(String userId, Pageable pageable);
    //注文詳細
    Optional<Orders> getOrderByIdAndUserId(Long orderId, String userId);
    //注文確認
   // OrderCheckResponseDto checkOrder(OrderRequestDto requestDto, String userId);

}
