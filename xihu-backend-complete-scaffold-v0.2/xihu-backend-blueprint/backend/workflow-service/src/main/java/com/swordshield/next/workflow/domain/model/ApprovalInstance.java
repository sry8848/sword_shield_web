package com.swordshield.next.workflow.domain.model;
/** 审批工作流拥有的业务实体标识。领域实现前不预设未经确认的字段。 */
public record ApprovalInstance(String id, String tenantId) {
    public ApprovalInstance {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id required");
        if (tenantId == null || tenantId.isBlank()) throw new IllegalArgumentException("tenantId required");
    }
}
