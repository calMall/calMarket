package com.example.calmall.global.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * アプリケーション全体で発生する例外を一括して処理するグローバル例外ハンドラー
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * DTOなどのバリデーションエラーを処理する
     * （@Valid付きのリクエストボディなどで発生）
     *
     * @param ex MethodArgumentNotValidException（Springが投げるバリデーション例外）
     * @return フィールドごとのエラーメッセージと400ステータス
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    /**
     * パスパラメータやリクエストパラメータのバリデーションエラー処理
     * （@Validated + @PathVariable, @RequestParam等）
     *
     * @param ex ConstraintViolationException
     * @return エラー内容と400ステータス
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                errors.put("param", violation.getMessage())
        );
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    /**
     * セッションやリクエストに必要な属性が見つからない場合の処理
     * （@SessionAttribute("user") が存在しない、@RequestAttribute が見つからない等）
     *
     * @param ex ServletRequestBindingException
     * @return エラーメッセージと401ステータス（未ログイン扱い）
     */
    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<Map<String, String>> handleServletRequestBindingException(ServletRequestBindingException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", "ログインが必要です");
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED); // 401
    }

    /**
     * その他すべての未処理例外のハンドリング（デバッグ用）
     *
     * @param ex 一般的な例外
     * @return エラーメッセージと500ステータス
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneralException(Exception ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}