// BE/src/main/java/com/example/calmall/orders/scheduler/OrderScheduler.java
package com.example.calmall.orders.scheduler;

import com.example.calmall.orders.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderScheduler {

    private final OrderService orderService;

    // 5秒ごとに実行
    @Scheduled(fixedRate = 5000)
    public void scheduleOrderStatusUpdate() {
        orderService.updateOrderStatus();
    }
}