package com.swordshield.next.common;
import java.util.Set;
/** Concrete policy obtained from role + membership + resource assignment. */
public record AuthorizationGrant(String tenantId, String permissionCode, String resourceType,
                                 ScopeKind scopeKind, Set<String> assignedResourceIds) {
    public AuthorizationGrant {
        if (tenantId == null || permissionCode == null || resourceType == null || scopeKind == null)
            throw new IllegalArgumentException("Incomplete grant");
        assignedResourceIds = Set.copyOf(assignedResourceIds == null ? Set.of() : assignedResourceIds);
    }
    public boolean permits(AuthorizationRequest request) {
        if (!tenantId.equals(request.tenantId()) || !permissionCode.equals(request.permissionCode())
            || !resourceType.equals(request.resourceType())) return false;
        return switch (scopeKind) {
            case NONE -> false;
            case ALL -> true; // caller must still enforce reliable tenant membership and tenant-owned resource
            case SELF -> request.ownerId() != null && request.actorId().equals(request.ownerId());
            case ASSIGNED -> assignedResourceIds.contains(request.resourceId());
        };
    }
}
