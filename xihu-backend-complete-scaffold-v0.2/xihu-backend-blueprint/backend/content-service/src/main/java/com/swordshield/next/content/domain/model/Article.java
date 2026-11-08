package com.swordshield.next.content.domain.model;
/** 内容与展商拥有的业务实体标识。领域实现前不预设未经确认的字段。 */
public record Article(String id, String tenantId) {
    public Article {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id required");
        if (tenantId == null || tenantId.isBlank()) throw new IllegalArgumentException("tenantId required");
    }
}
