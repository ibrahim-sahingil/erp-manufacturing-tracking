package com.uretimtakip.erp.common.exception;

import com.uretimtakip.erp.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Tum hatalar TEK MERKEZDE burada yakalanir ve standart JSON formatinda donulur.
 *
 * Bu sinif sayesinde her controller'da try-catch yazmana gerek kalmaz.
 * Bir service "throw new ResourceNotFoundException(...)" derse,
 * controller hicbir sey yapmadan bu sinif 404 cevabini hazirlar.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Kayit bulunamadi -> 404 Not Found
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), "RESOURCE_NOT_FOUND"));
    }

    /**
     * Is mantigi hatasi -> 400 Bad Request
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException ex) {
        log.warn("Business error: {} (code: {})", ex.getMessage(), ex.getErrorCode());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode()));
    }

    /**
     * @Valid ile validation hatasi -> 400 Bad Request
     * Hangi alanlarda hata var, detayli gosterir.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        log.warn("Validation failed: {}", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Dogrulama hatasi")
                        .errorCode("VALIDATION_ERROR")
                        .data(errors)
                        .build());
    }

    /**
     * DB constraint hatasi (unique, foreign key vs.) -> 409 Conflict
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.error("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(
                        "Veri butunlugu hatasi: Kayit zaten var veya iliskili kayit eksik.",
                        "DATA_INTEGRITY_ERROR"
                ));
    }

    /**
     * Login basarisiz (yanlis sifre) -> 401 Unauthorized
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Bad credentials attempt");
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Kullanici adi veya sifre hatali", "BAD_CREDENTIALS"));
    }

    /**
     * Yetki yok -> 403 Forbidden
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Bu islem icin yetkiniz yok", "ACCESS_DENIED"));
    }

    /**
     * Beklenmeyen hatalar -> 500 Internal Server Error
     * EN SONA: tum diger handler'lar buna yakalanmazsa buraya duser.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error: ", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "Beklenmeyen bir hata olustu. Lutfen tekrar deneyin.",
                        "INTERNAL_ERROR"
                ));
    }
}