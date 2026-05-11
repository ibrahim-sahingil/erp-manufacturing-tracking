package com.uretimtakip.erp.workspace;

import com.uretimtakip.erp.common.ApiResponse;
import com.uretimtakip.erp.workspace.dto.WorkspaceMemberRequest;
import com.uretimtakip.erp.workspace.dto.WorkspaceMemberResponse;
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
 * WorkspaceMember endpoints.
 *
 *   GET    /api/workspace-members?workspaceId=...   -> bir workspace'in uyeleri
 *   GET    /api/workspace-members?userId=...        -> bir kullanicinin workspace'leri
 *   POST   /api/workspace-members                   -> yeni uye ekle
 *   DELETE /api/workspace-members/{id}              -> uye cikar
 */
@RestController
@RequestMapping("/api/workspace-members")
@RequiredArgsConstructor
public class WorkspaceMemberController {

    private final WorkspaceMemberService memberService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkspaceMemberResponse>>> list(
            @RequestParam(required = false) UUID workspaceId,
            @RequestParam(required = false) UUID userId) {

        List<WorkspaceMemberResponse> members;
        if (workspaceId != null) {
            members = memberService.listByWorkspace(workspaceId);
        } else if (userId != null) {
            members = memberService.listByUser(userId);
        } else {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("workspaceId veya userId parametresi zorunlu", "MISSING_PARAM")
            );
        }

        return ResponseEntity.ok(ApiResponse.success(members));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WorkspaceMemberResponse>> add(
            @Valid @RequestBody WorkspaceMemberRequest request) {

        WorkspaceMemberResponse created = memberService.addMember(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Uye eklendi", created));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable UUID id) {
        memberService.removeMember(id);
        return ResponseEntity.ok(ApiResponse.success("Uye cikarildi", null));
    }
}