package com.example.calmall.orders.service;

import com.example.calmall.orders.dto.OrderRequestDto;
import com.example.calmall.orders.entity.Orders;
import com.example.calmall.orders.entity.OrderItem;
import com.example.calmall.orders.repository.OrdersRepository;
import com.example.calmall.product.entity.Product;
import com.example.calmall.product.repository.ProductRepository;
import com.example.calmall.user.entity.User;
import com.example.calmall.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrdersRepository ordersRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Orders createOrder(OrderRequestDto requestDto, String userId) {
        //userRepositoryからユーザー情報を取得
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("ログイン中のユーザーが見つかりません: " + userId));

        //新しい注文の作成
        Orders newOrder = Orders.builder()
                .user(user)
                .deliveryAddress(requestDto.getDeliveryAddress())
                .build();
        //注文商品の処理
        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderRequestDto.OrderItemDto itemDto : requestDto.getItems()) {
            Optional<Product> productOptional = productRepository.findByItemCode(itemDto.getItemCode());
            if (productOptional.isEmpty()) {
                throw new RuntimeException("商品が見つかりません: " + itemDto.getItemCode());
            }
            Product product = productOptional.get();

            if (product.getInventory() < itemDto.getQuantity()) {
                throw new RuntimeException("在庫不足: " + product.getItemName());
            }

            OrderItem orderItem = OrderItem.builder()
                    .order(newOrder)
                    .product(product)
                    .itemCode(product.getItemCode())
                    .itemName(product.getItemName())
                    .price(product.getPrice().doubleValue())
                    .quantity(itemDto.getQuantity())
                    .imageListUrls(String.join(",", product.getImages()))
                    .build();
            orderItems.add(orderItem);

            product.setInventory(product.getInventory() - itemDto.getQuantity());
        }
        //注文の保存
        newOrder.setOrderItems(orderItems);
        return ordersRepository.save(newOrder);
    }

    @Override
    @Transactional
    public void updateOrderStatus() {
        // すべての注文を取得
        List<Orders> allOrders = ordersRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Orders order : allOrders) {
            // PENDING（注文受付）から20秒以上経過した注文をSHIPPEDに更新
            if ("PENDING".equals(order.getStatus()) && order.getCreatedAt().plusSeconds(20).isBefore(now)) {
                order.setStatus("SHIPPED");
                ordersRepository.save(order);
            }
            // SHIPPED（発送済み）から30秒以上経過した注文をDELIVEREDに更新
            else if ("SHIPPED".equals(order.getStatus()) && order.getCreatedAt().plusSeconds(50).isBefore(now)) {
                order.setStatus("DELIVERED");
                ordersRepository.save(order);
            }
        }
    }


    //PENDING状態か確認
    @Override
    public boolean canCancel(Long orderId) {
        // 注文ステータスが "PENDING" の場合にtrueを返す
        return ordersRepository.findById(orderId)
                .map(order -> "PENDING".equals(order.getStatus()))
                .orElse(false);
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId) {
        // PENDING状態の注文をキャンセルしてCANCELLED状態にする。
        Orders order = ordersRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("注文が見つかりません: " + orderId));
        if (!canCancel(orderId)) {
            throw new RuntimeException("キャンセルは注文受付（PENDING）状態のときのみ可能です。");
        }
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            product.setInventory(product.getInventory() + item.getQuantity());
        }
        order.setStatus("CANCELLED");
        ordersRepository.save(order);
    }

    //DELIVEREDの状態か確認
    @Override
    public boolean canRefund(Long orderId) {
        // 注文ステータスが "DELIVERED" の場合にtrueを返す
        return ordersRepository.findById(orderId)
                .map(order -> "DELIVERED".equals(order.getStatus()))
                .orElse(false);
    }
    
    //DELIVERED状態の注文を払い戻してREFUNDEDの状態にする
    @Override
    @Transactional
    public void refundOrder(Long orderId) {
        Orders order = ordersRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("注文が見つかりません: " + orderId));
        if (!canRefund(orderId)) {
            throw new RuntimeException("払い戻しは配達完了（DELIVERED）状態のときのみ可能です。");
        }
        order.setStatus("REFUNDED");
        ordersRepository.save(order);
    }

    //指定されたuserIdを持つユーザーのすべての注文をリストとして取得
    @Override
    public List<Orders> findOrdersByUserId(String userId) {
        return ordersRepository.findByUser_UserId(userId);
    }

    //ページネーションを適用して取得
    @Override
    public Page<Orders> findOrdersByUserId(String userId, Pageable pageable) {
        return ordersRepository.findByUser_UserId(userId, pageable);
    }

    //特定のorderIdとuserIdの両方に一致する1件の注文を取得
    @Override
    public Optional<Orders> getOrderByIdAndUserId(Long orderId, String userId) {
        return ordersRepository.findByIdAndUser_UserId(orderId, userId);
    }

}