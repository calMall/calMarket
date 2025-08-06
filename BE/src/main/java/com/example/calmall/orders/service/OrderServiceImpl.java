package com.example.calmall.orders.service;

import com.example.calmall.cartitem.entity.CartItem;
import com.example.calmall.cartitem.repository.CartItemRepository;
import com.example.calmall.orders.dto.FinalOrderRequestDto;
import com.example.calmall.orders.dto.OrderCheckResponseDto;
import com.example.calmall.orders.entity.OrderItems;
import com.example.calmall.orders.entity.Orders;
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
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrdersRepository ordersRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;

    @Override
    @Transactional
    public OrderCheckResponseDto checkOrder(List<String> itemCodes, String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("ログイン中のユーザーが見つかりません: " + userId));

        //CartItemRepositoryにfindByUserIdAndItemCodeInメソッドを追加し、使用
        List<CartItem> cartItems = cartItemRepository.findByUserIdAndItemCodeIn(userId, itemCodes);
        if (cartItems.size() != itemCodes.size()) {
            throw new RuntimeException("指定された商品の一部がカートに見つからないか、選択されていません。");
        }

        //ProductリポジトリからitemCodesを使って商品情報をまとめて取得
        List<Product> products = productRepository.findByItemCodeIn(itemCodes);
        Map<String, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getItemCode, Function.identity()));

        List<String> unavailableItems = new ArrayList<>();
        int totalPrice = 0;

        for (CartItem cartItem : cartItems) {
            Product product = productMap.get(cartItem.getItemCode());
            if (product == null) {
                // ここには到達しないはずだが、念のためチェック
                unavailableItems.add("不明な商品: " + cartItem.getItemCode());
                continue;
            }

            if (product.getInventory() < cartItem.getQuantity()) {
                unavailableItems.add(product.getItemName());
            } else {
                totalPrice += product.getPrice().intValue() * cartItem.getQuantity();
            }
        }

        return OrderCheckResponseDto.builder()
                .isAvailable(unavailableItems.isEmpty())
                .unavailableItems(unavailableItems)
                .totalPrice(totalPrice)
                .build();
    }

    @Override
    @Transactional
    public void finalizeOrder(FinalOrderRequestDto requestDto, String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("ログイン中のユーザーが見つかりません: " + userId));

        List<String> itemCodes = requestDto.getItemCodes();
        List<CartItem> cartItems = cartItemRepository.findByUserIdAndItemCodeIn(userId, itemCodes);
    
        if (cartItems.size() != itemCodes.size()) {
            throw new RuntimeException("注文対象の商品がカートに見つかりません。");
        }
    
        List<Product> products = productRepository.findByItemCodeIn(itemCodes);
        Map<String, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getItemCode, Function.identity()));

        // 新しい注文を作成し、PENDINGで保存
        Orders newOrder = Orders.builder()
            .user(user)
            .deliveryAddress(requestDto.getDeliveryAddress())
            .status("PENDING")
            .orderItems(new ArrayList<>()) // ✅ ここに追記してください
            .build();
        ordersRepository.save(newOrder);

        // 注文アイテムを作成し、在庫を減らす
        for (CartItem cartItem : cartItems) {
            Product product = productMap.get(cartItem.getItemCode());
            if (product == null || product.getInventory() < cartItem.getQuantity()) {
                throw new RuntimeException("最終確認中に在庫が不足しました: " + (product != null ? product.getItemName() : cartItem.getItemCode()));
            }
        
            OrderItems orderItem = OrderItems.builder()
                .order(newOrder)
                .product(product)
                .itemName(product.getItemName())
                .priceAtOrder(product.getPrice().doubleValue())
                .quantity(cartItem.getQuantity())
                .imageListUrls(String.join(",", product.getImages()))
                .build();
        
            newOrder.getOrderItems().add(orderItem);
        
            // 在庫を減らす
            product.setInventory(product.getInventory() - cartItem.getQuantity());
            productRepository.save(product);
        }

        // 注文確定後、カートから商品をまとめて削除
        cartItemRepository.deleteByUserIdAndItemCodeIn(userId, itemCodes);
    
        ordersRepository.save(newOrder);
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
}