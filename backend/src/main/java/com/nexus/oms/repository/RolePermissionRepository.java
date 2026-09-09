package com.nexus.oms.repository;

import com.nexus.oms.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {

    List<RolePermission> findByTenantId(UUID tenantId);

    /**
     * Returns permissions for a role, merging tenant-specific rows with global
     * seed defaults (tenant_id IS NULL). Tenant rows sort first so a tenant-level
     * allow/deny overrides the global default (see PermissionService.hasPermission,
     * which short-circuits on the first match).
     */
    @Query("SELECT rp FROM RolePermission rp WHERE rp.role = :role AND (rp.tenantId = :tenantId OR rp.tenantId IS NULL) ORDER BY rp.tenantId NULLS LAST")
    List<RolePermission> findByTenantIdAndRole(@Param("tenantId") UUID tenantId, @Param("role") String role);

    List<RolePermission> findByTenantIdAndRoleAndPermissionGroup(UUID tenantId, String role, String permissionGroup);

    List<RolePermission> findByTenantIdAndPermissionGroup(UUID tenantId, String permissionGroup);
}
