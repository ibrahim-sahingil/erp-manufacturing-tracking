package com.uretimtakip.erp.workspace;

import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.workspace.dto.WorkspaceRequest;
import com.uretimtakip.erp.workspace.dto.WorkspaceResponse;
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
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> listAll() {
        return workspaceRepository.findAllByOrderBySortOrderAsc()
                .stream()
                .map(WorkspaceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WorkspaceResponse getById(UUID id) {
        return WorkspaceResponse.fromEntity(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> listByType(String type) {
        return workspaceRepository.findByType(type)
                .stream()
                .map(WorkspaceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public WorkspaceResponse create(WorkspaceRequest request) {
        if (workspaceRepository.existsByName(request.getName())) {
            throw new BusinessException(
                    "Bu isimde bir workspace zaten var: " + request.getName(),
                    "WORKSPACE_ALREADY_EXISTS"
            );
        }

        Workspace workspace = Workspace.builder()
                .name(request.getName())
                .type(request.getType() != null ? request.getType() : "area")
                .description(request.getDescription())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 1)
                .build();

        Workspace saved = workspaceRepository.save(workspace);
        log.info("Workspace created: id={}, name={}", saved.getId(), saved.getName());

        return WorkspaceResponse.fromEntity(saved);
    }

    @Transactional
    public WorkspaceResponse update(UUID id, WorkspaceRequest request) {
        Workspace workspace = findEntityById(id);

        if (!workspace.getName().equals(request.getName())
                && workspaceRepository.existsByName(request.getName())) {
            throw new BusinessException(
                    "Bu isimde bir workspace zaten var: " + request.getName(),
                    "WORKSPACE_ALREADY_EXISTS"
            );
        }

        workspace.setName(request.getName());
        if (request.getType() != null) workspace.setType(request.getType());
        workspace.setDescription(request.getDescription());
        if (request.getSortOrder() != null) workspace.setSortOrder(request.getSortOrder());

        Workspace updated = workspaceRepository.save(workspace);
        log.info("Workspace updated: id={}, name={}", updated.getId(), updated.getName());

        return WorkspaceResponse.fromEntity(updated);
    }

    @Transactional
    public void delete(UUID id) {
        Workspace workspace = findEntityById(id);
        workspaceRepository.delete(workspace);
        log.info("Workspace deleted: id={}, name={}", id, workspace.getName());
    }

    private Workspace findEntityById(UUID id) {
        return workspaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", "id", id));
    }
}