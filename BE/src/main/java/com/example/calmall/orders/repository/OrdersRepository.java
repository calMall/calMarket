package com.example.calmall.orders.repository;

import com.example.calmall.orders.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

// 注文テーブルの操作を行うリポジトリ
public interface OrdersRepository extends JpaRepository<Orders, Long> {}
