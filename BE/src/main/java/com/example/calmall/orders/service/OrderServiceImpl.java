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
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("ログイン中のユーザーが見つかりません: " + userId));

        Orders newOrder = Orders.builder()
                .user(user)
                .deliveryAddress(requestDto.getDeliveryAddress())
                .build();

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

        newOrder.setOrderItems(orderItems);
        return ordersRepository.save(newOrder);
    }

    @Override
    public List<Orders> getAllOrders() {
        return ordersRepository.findAll();
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

}