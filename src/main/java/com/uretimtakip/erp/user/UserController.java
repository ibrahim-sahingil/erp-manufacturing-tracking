package com.uretimtakip.erp.user;

import com.uretimtakip.erp.common.ApiResponse;
import com.uretimtakip.erp.user.dto.UserRequest;
import com.uretimtakip.erp.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * User REST endpoint'leri.
 *
 * Endpoints:
 *   GET    /api/users        -> Tum kullanicilar
 *   GET    /api/users/{id}   -> Tek kullanici
 *   POST   /api/users        -> Yeni kullanici olustur
 *   PUT    /api/users/{id}   -> Guncelle
 *   DELETE /api/users/{id}   -> Sil
 *
 * Frontend'in hem "users" (personel) hem "app_users" (giris kullanicilari)
 * cagirilari bu tek controller'a gelir (bkz. UserResponse alias alanlari).
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(userService.listAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(
            @Valid @RequestBody UserRequest request) {

        UserResponse created = userService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Kullanici olusturuldu", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UserRequest request) {

        UserResponse updated = userService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Kullanici guncellendi", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Kullanici silindi", null));
    }
}
