package com.example.calmall.user.repository;

import com.example.calmall.user.entity.DeliveryAddress;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 配送先住所のリポジトリ
 */
public interface DeliveryAddressRepository extends JpaRepository<DeliveryAddress, Long> {
}
