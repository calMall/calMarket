package com.example.calmall.user.repository;

import com.example.calmall.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * ユーザーデータベース操作用リポジトリ
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** emailでユーザーを検索する */
    Optional<User> findByEmail(String email);

    /** userId（UUID）でユーザーを検索し、配送先住所も一括取得 */
    @EntityGraph(attributePaths = "deliveryAddresses")
    Optional<User> findByUserId(String userId);

    /** emailの重複チェック */
    boolean existsByEmail(String email);
}
