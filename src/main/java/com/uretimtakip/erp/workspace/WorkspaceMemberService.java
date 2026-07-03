package com.uretimtakip.erp.workspace;

import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.workspace.dto.WorkspaceMemberRequest;
import com.uretimtakip.erp.workspace.dto.WorkspaceMemberResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceMemberService {

    private final WorkspaceMemberRepository memberRepository;

    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> listAll() {
        return memberRepository.findAll()
                .stream()
                .map(WorkspaceMemberResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> listByWorkspace(UUID workspaceId) {
        return memberRepository.findByWorkspaceId(workspaceId)
                .stream()
                .map(WorkspaceMemberResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> listByUser(UUID userId) {
        return memberRepository.findByUserId(userId)
                .stream()
                .map(WorkspaceMemberResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public WorkspaceMemberResponse addMember(WorkspaceMemberRequest request) {
        if (memberRepository.existsByWorkspaceIdAndUserId(
                request.getWorkspaceId(), request.getUserId())) {
            throw new BusinessException(
                    "Bu kullanici zaten bu workspace'in uyesi",
                    "MEMBER_ALREADY_EXISTS"
            );
        }

        WorkspaceMember member = WorkspaceMember.builder()
                .workspaceId(request.getWorkspaceId())
                .userId(request.getUserId())
                .role(request.getRole())
                .build();

        WorkspaceMember saved = memberRepository.save(member);
        log.info("WorkspaceMember added: workspaceId={}, userId={}",
                saved.getWorkspaceId(), saved.getUserId());

        return WorkspaceMemberResponse.fromEntity(saved);
    }

    @Transactional
    public void removeMember(UUID id) {
        WorkspaceMember member = memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkspaceMember", "id", id));

        memberRepository.delete(member);
        log.info("WorkspaceMember removed: id={}", id);
    }
}