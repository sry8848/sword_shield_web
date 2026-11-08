package com.swordshield.next.common;
public record ResourceRef(String resourceType, String resourceId) {
    public ResourceRef {
        if (resourceType == null || resourceType.isBlank() || resourceId == null || resourceId.isBlank())
            throw new IllegalArgumentException("resourceType and resourceId are required");
    }
}
