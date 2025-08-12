package com.example.calmall.orders.service;

import com.example.calmall.cartitem.service.CartItemService;
import com.example.calmall.orders.dto.OrderCheckResponseDto;
import com.example.calmall.orders.dto.OrderRequestDto;
import com.example.calmall.orders.entity.Orders;
import com.example.calmall.orders.entity.OrderItems;
import com.example.calmall.orders.repository.OrdersRepository;
import com.example.calmall.product.entity.Product;
import com.example.calmall.product.repository.ProductRepository;
import com.example.calmall.user.entity.User;
import com.example.calmall.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrdersRepository ordersRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartItemService cartItemService;

    @Override
    @Transactional
    public Orders createOrder(OrderRequestDto requestDto, String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("ログイン中のユーザーが見つかりません: " + userId));

        Orders newOrder = Orders.builder()
                .user(user)
                .deliveryAddress(requestDto.getDeliveryAddress())
                .build();

        List<OrderItems> orderItems = new ArrayList<>();
        for (OrderRequestDto.OrderItemDto itemDto : requestDto.getItems()) {
            Optional<Product> productOptional = productRepository.findByItemCode(itemDto.getItemCode());
            if (productOptional.isEmpty()) {
                throw new RuntimeException("商品が見つかりません: " + itemDto.getItemCode());
            }
            Product product = productOptional.get();

            if (product.getInventory() < itemDto.getQuantity()) {
                throw new RuntimeException("在庫不足: " + product.getItemName());
            }

            OrderItems orderItem = OrderItems.builder()
                    .order(newOrder)
                    .product(product)
                    .itemName(product.getItemName())
                    .priceAtOrder(product.getPrice().doubleValue())
                    .quantity(itemDto.getQuantity())
                    .imageListUrls(String.join(",", product.getImages()))
                    .build();
            orderItems.add(orderItem);

            product.setInventory(product.getInventory() - itemDto.getQuantity());
        }

        newOrder.setOrderItems(orderItems);
        // ここで注文を保存し、返り値を受け取ります。
        Orders savedOrder = ordersRepository.save(newOrder);

        // 注文が確定した商品のitemCodeリストを抽出
        List<String> orderedItemCodes = requestDto.getItems().stream()
                .map(OrderRequestDto.OrderItemDto::getItemCode)
                .collect(Collectors.toList());
        
        // CartItemServiceを呼び出して、該当商品をカートから削除
        cartItemService.removeCartItemsByItemCodes(orderedItemCodes, userId);

        // 最後に、保存した注文を返します。
        return savedOrder;
    }

    @Override
    @Transactional
    public void updateOrderStatus() {
        List<Orders> allOrders = ordersRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Orders order : allOrders) {
            if ("PENDING".equals(order.getStatus()) && order.getCreatedAt().plusSeconds(20).isBefore(now)) {
                order.setStatus("SHIPPED");
                ordersRepository.save(order);
            } else if ("SHIPPED".equals(order.getStatus()) && order.getCreatedAt().plusSeconds(50).isBefore(now)) {
                order.setStatus("DELIVERED");
                ordersRepository.save(order);
            }
        }
    }

    @Override
    public boolean canCancel(Long orderId, String userId) {
        return ordersRepository.findByIdAndUser_UserId(orderId, userId)
                .map(order -> "PENDING".equals(order.getStatus()))
                .orElse(false);
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId, String userId) {
        Orders order = ordersRepository.findByIdAndUser_UserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("注文が見つからないか、アクセス権限がありません。"));

        if (!canCancel(orderId, userId)) {
            throw new RuntimeException("キャンセルは注文受付（PENDING）状態のときのみ可能です。");
        }

        for (OrderItems item : order.getOrderItems()) {
            Product product = item.getProduct();
            product.setInventory(product.getInventory() + item.getQuantity());
        }
        order.setStatus("CANCELLED");
        ordersRepository.save(order);
    }

    @Override
    public boolean canRefund(Long orderId, String userId) {
        return ordersRepository.findByIdAndUser_UserId(orderId, userId)
                .map(order -> "DELIVERED".equals(order.getStatus()))
                .orElse(false);
    }

    @Override
    @Transactional
    public void refundOrder(Long orderId, String userId) {
        Orders order = ordersRepository.findByIdAndUser_UserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("注文が見つからないか、アクセス権限がありません。"));
        if (!canRefund(orderId, userId)) {
            throw new RuntimeException("払い戻しは配達完了（DELIVERED）状態のときのみ可能です。");
        }
        order.setStatus("REFUNDED");
        ordersRepository.save(order);
    }

    @Override
    public List<Orders> findOrdersByUserId(String userId) {
        return ordersRepository.findByUser_UserId(userId);
    }

    @Override
    public Page<Orders> findOrdersByUserId(String userId, Pageable pageable) {
        return ordersRepository.findByUser_UserId(userId, pageable);
    }

    @Override
    public Optional<Orders> getOrderByIdAndUserId(Long orderId, String userId) {
        return ordersRepository.findByIdAndUser_UserId(orderId, userId);
    }
/* 
    @Override
    public OrderCheckResponseDto checkOrder(OrderRequestDto requestDto, String userId) {
        OrderCheckResponseDto response = new OrderCheckResponseDto();
        response.setMessage("success");
        Long totalPrice = 0L;
        Map<String, Integer> insufficientItems = new HashMap<>();

        for (OrderRequestDto.OrderItemDto itemDto : requestDto.getItems()) {
            Product product = productRepository.findByItemCode(itemDto.getItemCode())
                .orElseThrow(() -> new RuntimeException("商品が見つかりません: " + itemDto.getItemCode()));

            if (product.getInventory() < itemDto.getQuantity()) {
                insufficientItems.put(product.getItemName(), product.getInventory());
                response.setMessage("fail: 在庫が不足しています");
            }

            totalPrice += product.getPrice() * itemDto.getQuantity();
        }

        response.setTotalPrice(totalPrice);
        response.setInsufficientItems(insufficientItems);
        return response;
    }
    */
}