package com.swordshield.next.limitedseat.domain.model;
/** 限量预约拥有的业务实体标识。领域实现前不预设未经确认的字段。 */
public record ReservationBatch(String id, String tenantId) {
    public ReservationBatch {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id required");
        if (tenantId == null || tenantId.isBlank()) throw new IllegalArgumentException("tenantId required");
    }
}
