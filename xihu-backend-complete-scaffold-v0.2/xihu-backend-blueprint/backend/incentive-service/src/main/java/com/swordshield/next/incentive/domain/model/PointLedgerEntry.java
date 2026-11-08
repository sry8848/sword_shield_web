package com.swordshield.next.incentive.domain.model;
/** 参与激励拥有的业务实体标识。领域实现前不预设未经确认的字段。 */
public record PointLedgerEntry(String id, String tenantId) {
    public PointLedgerEntry {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id required");
        if (tenantId == null || tenantId.isBlank()) throw new IllegalArgumentException("tenantId required");
    }
}
