package com.example.calmall.orders.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.calmall.global.dto.ApiResponseDto;
import com.example.calmall.orders.dto.OrderDetailResponseDto;
import com.example.calmall.orders.dto.OrderDetailResponseDto.OrderDetail;
import com.example.calmall.orders.dto.OrderDetailResponseDto.OrderItemDto;
import com.example.calmall.orders.dto.OrderListResponseDto;
import com.example.calmall.orders.dto.OrderRequestDto;
import com.example.calmall.orders.entity.Orders;
import com.example.calmall.orders.service.OrderService;
import com.example.calmall.user.entity.User;
import com.example.calmall.orders.dto.OrderListResponseDto.OrderSummary;

import jakarta.servlet.http.HttpSession; // 追加
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;
    //注文作成
    @PostMapping
    public ResponseEntity<ApiResponseDto> createOrder(@RequestBody OrderRequestDto requestDto, HttpSession session) {
        
        User user = (User) session.getAttribute("user");
        String userId = user.getUserId();
     //   String userId = (String) session.getAttribute("userId");
        //ユーザ確認
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDto("fail: ログインが必要です"));
        }
        
        try {
            orderService.createOrder(requestDto, userId);
            return ResponseEntity.ok(new ApiResponseDto("success"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDto("fail: " + e.getMessage()));
        }
    }
    //ステータスがFENDINGの場合キャンセル
    @PostMapping("/cancel/{orderId}")
    public ResponseEntity<ApiResponseDto> cancelOrder(@PathVariable Long orderId, HttpSession session) {
        //ユーザ確認
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDto("fail: ログインが必要です"));
        }
        try {
            orderService.cancelOrder(orderId);
            return ResponseEntity.ok(new ApiResponseDto("success"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponseDto("fail: " + e.getMessage()));
        }
    }
    //ステータスがDELIVEREDの場合払い戻し
    @PostMapping("/refund/{orderId}")
    public ResponseEntity<ApiResponseDto> refundOrder(@PathVariable Long orderId, HttpSession session) {
        //ユーザ確認
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDto("fail: ログインが必要です"));
        }
        try {
            orderService.refundOrder(orderId);
            return ResponseEntity.ok(new ApiResponseDto("success"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDto("fail: " + e.getMessage()));
        }
    }
    //ユーザ注文履歴
    @GetMapping("/history")
    public ResponseEntity<OrderListResponseDto> getOrderHistory(
            HttpSession session,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        //ユーザ確認
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                OrderListResponseDto.builder().message("fail").build());
        }
        //ページネーション
        Pageable pageable = PageRequest.of(page, size);
        Page<Orders> ordersPage = orderService.findOrdersByUserId(user.getUserId(), pageable);
        
        List<OrderSummary> orderSummaries = ordersPage.getContent().stream()
            .flatMap(order -> order.getOrderItems().stream()
                .map(item -> OrderSummary.builder()
                    .orderId(order.getId())
                    .itemCode(item.getItemCode())
                    .itemName(item.getItemName())
                    .price(item.getPrice().intValue())
                    .quantity(item.getQuantity())
                    .date(order.getCreatedAt().toLocalDate().toString())
                    .imageList(List.of(item.getImageListUrls().split(",")))
                    .orderDate(order.getCreatedAt().toString())
                    .build()))
            .collect(Collectors.toList());

        OrderListResponseDto responseDto = OrderListResponseDto.builder()
            .message("success")
            .orders(orderSummaries)
            .build();

        return ResponseEntity.ok(responseDto);
    }

    //ユーザ注文詳細
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponseDto> getOrderDetails(@PathVariable Long orderId, HttpSession session) {
        //ユーザ確認
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                OrderDetailResponseDto.builder().message("fail").build());
        }

        Optional<Orders> orderOptional = orderService.getOrderByIdAndUserId(orderId, user.getUserId());

        if (orderOptional.isPresent()) {
            Orders order = orderOptional.get();

            List<OrderItemDto> itemDtos = order.getOrderItems().stream()
                    .map(item -> OrderItemDto.builder()
                            .itemCode(item.getItemCode())
                            .itemName(item.getItemName())
                            .price(item.getPrice().intValue())
                            .quantity(item.getQuantity())
                            .imageList(List.of(item.getImageListUrls().split(",")))
                            .build())
                    .collect(Collectors.toList());

            OrderDetail orderDetail = OrderDetail.builder()
                    .orderId(order.getId())
                    .deliveryAddress(order.getDeliveryAddress())
                    .orderDate(order.getCreatedAt())
                    .status(order.getStatus())
                    .orderItems(itemDtos)
                    .build();

            return ResponseEntity.ok(OrderDetailResponseDto.builder()
                    .message("success")
                    .order(orderDetail)
                    .build());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                OrderDetailResponseDto.builder().message("fail").build());
        }
    }

}