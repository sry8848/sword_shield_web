package com.swordshield.next.activity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
@TableName("event")
public class EventEntity {
    @TableId private String id;
    @TableField("tenant_id") private String tenantId;
    private String title;
    private String status;
    @TableField("created_by") private String createdBy;
    public EventEntity() {}
    public String getId() { return id; }
    public void setId(String id) { this.id=id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId=tenantId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title=title; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status=status; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy=createdBy; }
}
