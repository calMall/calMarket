package com.example.calmall.user.repository;

import com.example.calmall.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * ユーザーデータベース操作用リポジトリ
 */
public interface UserRepository extends JpaRepository<User, Long> {

    // emailで検索
    Optional<User> findByEmail(String email);

    // userId（UUID）で検索
    Optional<User> findByUserId(String userId);

    // emailの重複確認
    boolean existsByEmail(String email);
}
