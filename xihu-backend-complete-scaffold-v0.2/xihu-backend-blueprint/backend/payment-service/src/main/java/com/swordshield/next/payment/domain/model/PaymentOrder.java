package com.swordshield.next.payment.domain.model;
/** 支付与退款拥有的业务实体标识。领域实现前不预设未经确认的字段。 */
public record PaymentOrder(String id, String tenantId) {
    public PaymentOrder {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id required");
        if (tenantId == null || tenantId.isBlank()) throw new IllegalArgumentException("tenantId required");
    }
}
