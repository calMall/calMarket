package com.example.calmall.orders.service;

import com.example.calmall.orders.dto.OrderRequestDto;
import com.example.calmall.orders.entity.Orders;
import java.util.List;

public interface OrderService {
    Orders createOrder(OrderRequestDto requestDto, String userId);
    
    List<Orders> getAllOrders();
    
    //注文状況
    void updateOrderStatus();
}
