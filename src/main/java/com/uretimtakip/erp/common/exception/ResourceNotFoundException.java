package com.uretimtakip.erp.common.exception;

/**
 * Veritabaninda istenen kayit bulunamadiginda firlatilir.
 *
 * Ornek: User userById(UUID id) -> kullanici yoksa
 *        throw new ResourceNotFoundException("User", "id", id);
 *
 * GlobalExceptionHandler bu hatayi 404 Not Found olarak yakalar.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s bulunamadi (%s: '%s')", resourceName, fieldName, fieldValue));
    }
}