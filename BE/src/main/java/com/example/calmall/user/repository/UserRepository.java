package com.example.calmall.user.repository;

import com.example.calmall.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

// ユーザーテーブルの操作を行うリポジトリ
public interface UserRepository extends JpaRepository<User, Long> {}
