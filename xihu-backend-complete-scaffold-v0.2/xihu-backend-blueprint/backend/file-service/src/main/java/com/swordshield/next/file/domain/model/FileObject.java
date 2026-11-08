package com.swordshield.next.file.domain.model;
/** 文件资产拥有的业务实体标识。领域实现前不预设未经确认的字段。 */
public record FileObject(String id, String tenantId) {
    public FileObject {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id required");
        if (tenantId == null || tenantId.isBlank()) throw new IllegalArgumentException("tenantId required");
    }
}
