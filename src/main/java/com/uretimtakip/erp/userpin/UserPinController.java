package com.uretimtakip.erp.userpin;

import com.uretimtakip.erp.common.ApiResponse;
import com.uretimtakip.erp.userpin.dto.UserPinRequest;
import com.uretimtakip.erp.userpin.dto.UserPinResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * UserPin REST endpoint'leri.
 *
 * Endpoints:
 *   GET    /api/user-pins?userId=xxx  -> Kullanicinin pinleri (userId ZORUNLU)
 *   POST   /api/user-pins             -> Pin ekle
 *   DELETE /api/user-pins/{id}        -> Pin kaldir
 */
@RestController
@RequestMapping("/api/user-pins")
@RequiredArgsConstructor
public class UserPinController {

    private final UserPinService userPinService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserPinResponse>>> list(
            @RequestParam UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(userPinService.listByUser(userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserPinResponse>> create(
            @Valid @RequestBody UserPinRequest request) {

        UserPinResponse created = userPinService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Pin eklendi", created));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        userPinService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Pin kaldirildi", null));
    }
}
