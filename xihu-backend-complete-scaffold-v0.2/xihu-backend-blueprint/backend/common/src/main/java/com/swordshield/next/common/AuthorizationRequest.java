package com.swordshield.next.common;
/** Resource identity and actor; ownerId is NOT inferred from resourceId. */
public record AuthorizationRequest(String actorId, String tenantId, String permissionCode,
                                    String resourceType, String resourceId, String ownerId) {
    public AuthorizationRequest {
        if (actorId == null || actorId.isBlank() || tenantId == null || tenantId.isBlank()
            || permissionCode == null || permissionCode.isBlank()
            || resourceType == null || resourceType.isBlank() || resourceId == null || resourceId.isBlank())
            throw new IllegalArgumentException("Missing authorization context");
    }
}
