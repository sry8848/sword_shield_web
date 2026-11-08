package com.swordshield.next.interaction.domain.model;
/** 互动交流拥有的业务实体标识。领域实现前不预设未经确认的字段。 */
public record Comment(String id, String tenantId) {
    public Comment {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id required");
        if (tenantId == null || tenantId.isBlank()) throw new IllegalArgumentException("tenantId required");
    }
}
