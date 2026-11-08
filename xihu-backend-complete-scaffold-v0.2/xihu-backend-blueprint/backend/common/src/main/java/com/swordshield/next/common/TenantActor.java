package com.swordshield.next.common;
import java.util.Set;
/** Tenant id comes from verified server-side identity, never an untrusted request header. */
public record TenantActor(String accountId, String tenantId, Set<String> permissions) {
    public TenantActor {
        if (accountId == null || accountId.isBlank() || tenantId == null || tenantId.isBlank())
            throw new IllegalArgumentException("Invalid identity context");
        permissions = Set.copyOf(permissions);
    }
    public boolean has(String permission) { return permissions.contains(permission); }
}
