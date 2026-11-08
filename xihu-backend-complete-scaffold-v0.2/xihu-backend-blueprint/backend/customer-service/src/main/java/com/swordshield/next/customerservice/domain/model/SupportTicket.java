package com.swordshield.next.customerservice.domain.model;
/** 客服与工单拥有的业务实体标识。领域实现前不预设未经确认的字段。 */
public record SupportTicket(String id, String tenantId) {
    public SupportTicket {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id required");
        if (tenantId == null || tenantId.isBlank()) throw new IllegalArgumentException("tenantId required");
    }
}
