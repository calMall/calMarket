package com.example.calmall.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 所持ポイント更新用リクエストDTO(余裕があれば実装)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPointUpdateRequestDto {

    // ユーザーID
    private Long userId;

    // 加算・減算するポイント数
    private int point;
}
