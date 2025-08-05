package com.example.calmall.orders.controller;

import com.example.calmall.global.dto.ApiResponseDto;
import com.example.calmall.orders.dto.OrderRequestDto;
import com.example.calmall.orders.service.OrderService;
import com.example.calmall.user.entity.User;

import jakarta.servlet.http.HttpSession; // 追加
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponseDto> createOrder(@RequestBody OrderRequestDto requestDto, HttpSession session) {
        User user = (User) session.getAttribute("user");
        String userId = user.getUserId();
     //   String userId = (String) session.getAttribute("userId");
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
}