package com.uretimtakip.erp.common.exception;

/**
 * Is mantigi hatalarinda firlatilir.
 *
 * Ornekler:
 *   - "Sifre yanlis"
 *   - "Bu siparis zaten onaylanmis"
 *   - "Stok yetersiz: parca X"
 *   - "Kullanici aktif degil"
 *
 * GlobalExceptionHandler bu hatayi 400 Bad Request olarak yakalar.
 */
public class BusinessException extends RuntimeException {

    private final String errorCode;

    public BusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
    }

    public BusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}