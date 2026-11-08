package com.swordshield.next.organization.domain.model;
/** 组织与部门拥有的业务实体标识。领域实现前不预设未经确认的字段。 */
public record OrgUnit(String id, String tenantId) {
    public OrgUnit {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id required");
        if (tenantId == null || tenantId.isBlank()) throw new IllegalArgumentException("tenantId required");
    }
}
