package com.example.calmall.user.service;

import com.example.calmall.global.dto.ApiResponseDto;
import com.example.calmall.user.dto.*;
import org.springframework.http.ResponseEntity;

public interface UserService {

    ResponseEntity<ApiResponseDto> register(UserRegisterRequestDto requestDto);

    UserLoginResponseDto login(UserLoginRequestDto requestDto);

    ResponseEntity<ApiResponseDto> logout(UserLogoutRequestDto requestDto);

    ResponseEntity<UserDetailResponseDto> getUserDetail(String userId);

    ResponseEntity<RefundResponseDto> refund(RefundRequestDto requestDto);

    ResponseEntity<ApiResponseDto> addAddress(UserAddressRequestDto requestDto);
}
