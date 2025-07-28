package com.example.calmall.global.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * アップロード用のディレクトリをアプリ起動時に作成する設定クラス
 */
@Component
@RequiredArgsConstructor
public class FileUploadConfig {

    // application.properties で設定した保存先
    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * アプリケーション起動時に upload ディレクトリを作成
     */
    @PostConstruct
    public void init() {
        File uploadPath = new File(uploadDir);
        if (!uploadPath.exists()) {
            boolean created = uploadPath.mkdirs();
            if (created) {
                System.out.println("✅ アップロードディレクトリ作成成功: " + uploadDir);
            } else {
                System.err.println("❌ アップロードディレクトリ作成失敗: " + uploadDir);
            }
        }
    }
}
