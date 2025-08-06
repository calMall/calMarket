package com.example.calmall.orders.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.calmall.global.dto.ApiResponseDto;
import com.example.calmall.orders.dto.OrderDetailResponseDto;
import com.example.calmall.orders.dto.OrderDetailResponseDto.OrderDetail;
import com.example.calmall.orders.dto.OrderDetailResponseDto.OrderItemDto;
import com.example.calmall.orders.dto.OrderListResponseDto;
// import com.example.calmall.orders.dto.OrderRequestDto; // 従来のDTOは削除
import com.example.calmall.orders.dto.OrderListResponseDto.OrderSummary;
import com.example.calmall.orders.dto.FinalOrderRequestDto;
import com.example.calmall.orders.dto.OrderCheckResponseDto;
import com.example.calmall.orders.dto.ProvisionalOrderRequestDto; // 仮注文リクエストDTOを追加

import com.example.calmall.orders.entity.Orders;
import com.example.calmall.orders.service.OrderService;
import com.example.calmall.user.entity.User;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    private User getUserFromSession(HttpSession session) {
        return (User) session.getAttribute("user");
    }

    private ResponseEntity<ApiResponseDto> createErrorResponse(String message, HttpStatus status) {
        return ResponseEntity.status(status).body(new ApiResponseDto("fail: " + message));
    }

    // 注文前チェックAPI（仮注文の作成）
    @PostMapping("/provisional")
    public ResponseEntity<?> checkOrder(@RequestBody ProvisionalOrderRequestDto requestDto, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) {
            return createErrorResponse("ログインが必要です", HttpStatus.UNAUTHORIZED);
        }
        try {
            // Service層で在庫チェックと合計金額の計算を行う
            OrderCheckResponseDto responseDto = orderService.checkOrder(requestDto.getItemCodes(), user.getUserId());
            return ResponseEntity.ok(responseDto);
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // 注文確定API
    @PostMapping("/finalize")
    public ResponseEntity<ApiResponseDto> finalizeOrder(@RequestBody FinalOrderRequestDto requestDto, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) {
            return createErrorResponse("ログインが必要です", HttpStatus.UNAUTHORIZED);
        }
        try {
            orderService.finalizeOrder(requestDto, user.getUserId());
            return ResponseEntity.ok(new ApiResponseDto("success"));
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/cancel/{orderId}")
    public ResponseEntity<ApiResponseDto> cancelOrder(@PathVariable Long orderId, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) {
            return createErrorResponse("ログインが必要です", HttpStatus.UNAUTHORIZED);
        }
        try {
            orderService.cancelOrder(orderId, user.getUserId());
            return ResponseEntity.ok(new ApiResponseDto("success"));
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/refund/{orderId}")
    public ResponseEntity<ApiResponseDto> refundOrder(@PathVariable Long orderId, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) {
            return createErrorResponse("ログインが必要です", HttpStatus.UNAUTHORIZED);
        }
        try {
            orderService.refundOrder(orderId, user.getUserId());
            return ResponseEntity.ok(new ApiResponseDto("success"));
        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/history")
    public ResponseEntity<OrderListResponseDto> getOrderHistory(
            HttpSession session,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        User user = getUserFromSession(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                OrderListResponseDto.builder().message("fail").build());
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Orders> ordersPage = orderService.findOrdersByUserId(user.getUserId(), pageable);
        
        List<OrderSummary> orderSummaries = ordersPage.getContent().stream()
            .flatMap(order -> order.getOrderItems().stream()
                .map(item -> OrderSummary.builder()
                    .orderId(order.getId())
                    .itemCode(item.getProduct().getItemCode())
                    .itemName(item.getItemName())
                    .price(item.getPriceAtOrder().intValue())
                    .quantity(item.getQuantity())
                    .date(order.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE))
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

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponseDto> getOrderDetails(@PathVariable Long orderId, HttpSession session) {
        User user = getUserFromSession(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                OrderDetailResponseDto.builder().message("fail").build());
        }

        Optional<Orders> orderOptional = orderService.getOrderByIdAndUserId(orderId, user.getUserId());

        if (orderOptional.isPresent()) {
            Orders order = orderOptional.get();

            List<OrderItemDto> itemDtos = order.getOrderItems().stream()
                .map(item -> OrderItemDto.builder()
                    .itemCode(item.getProduct().getItemCode())
                    .itemName(item.getItemName())
                    .price(item.getPriceAtOrder().intValue())
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