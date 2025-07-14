package com.example.calmall.repository;

import com.example.calmall.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

// ユーザーテーブルの操作を行うリポジトリ
public interface UserRepository extends JpaRepository<User, Long> {}
