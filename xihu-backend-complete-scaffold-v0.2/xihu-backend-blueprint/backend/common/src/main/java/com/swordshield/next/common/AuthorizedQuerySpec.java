package com.swordshield.next.common;
import java.util.Set;
/** Resolved query constraint: apply at SQL layer BEFORE count/paging/export. */
public record AuthorizedQuerySpec(String tenantId, String permissionCode, String resourceType,
                                  ScopeKind scopeKind, Set<String> resourceIds, String actorId) {
    public AuthorizedQuerySpec {
        if (tenantId == null || permissionCode == null || resourceType == null || scopeKind == null)
            throw new IllegalArgumentException("Missing query scope");
        resourceIds = Set.copyOf(resourceIds == null ? Set.of() : resourceIds);
    }
}
