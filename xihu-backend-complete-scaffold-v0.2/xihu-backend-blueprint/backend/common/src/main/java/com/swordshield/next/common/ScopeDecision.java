package com.swordshield.next.common;
import java.util.Set;
public record ScopeDecision(String permissionCode, String resourceType, ScopeKind kind, Set<String> resourceIds) {
    public ScopeDecision {
        if (permissionCode == null || resourceType == null || kind == null) throw new IllegalArgumentException();
        resourceIds = Set.copyOf(resourceIds == null ? Set.of() : resourceIds);
    }
    public boolean allows(String actorId, ResourceRef ref) {
        if (!resourceType.equals(ref.resourceType())) return false;
        return switch (kind) {
            case ALL -> true;
            case NONE -> false;
            case SELF -> actorId.equals(ref.resourceId());
            case ASSIGNED -> resourceIds.contains(ref.resourceId());
        };
    }
}
