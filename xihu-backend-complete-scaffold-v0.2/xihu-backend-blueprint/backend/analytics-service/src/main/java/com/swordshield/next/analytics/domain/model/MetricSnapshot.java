package com.swordshield.next.analytics.domain.model;
/** 运营分析拥有的业务实体标识。领域实现前不预设未经确认的字段。 */
public record MetricSnapshot(String id, String tenantId) {
    public MetricSnapshot {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id required");
        if (tenantId == null || tenantId.isBlank()) throw new IllegalArgumentException("tenantId required");
    }
}
