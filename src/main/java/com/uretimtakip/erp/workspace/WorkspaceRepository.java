package com.uretimtakip.erp.workspace;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    List<Workspace> findAllByOrderBySortOrderAsc();

    List<Workspace> findByType(String type);

    boolean existsByName(String name);
}