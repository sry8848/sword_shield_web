package com.swordshield.next.common;
/** Verifies the resource is in the current tenant before applying role/department scopes. */
public final class TenantOwnership {
    public boolean sameTenant(String actorTenantId, String resourceTenantId) {
        return actorTenantId != null && actorTenantId.equals(resourceTenantId);
    }
}
