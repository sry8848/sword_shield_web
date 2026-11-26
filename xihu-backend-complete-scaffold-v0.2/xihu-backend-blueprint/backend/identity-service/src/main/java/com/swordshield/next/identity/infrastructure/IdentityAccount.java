package com.swordshield.next.identity.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("identity_account")
class IdentityAccount {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String phone;
    private String passwordHash;
    private String status;
    private Integer credentialVersion;
    private Integer permissionVersion;

    public String getId() {
        return id;
    }

    public String getPhone() {
        return phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isEnabled() {
        return "ENABLED".equals(status);
    }

    public int getCredentialVersion() {
        return credentialVersion;
    }

    public int getPermissionVersion() {
        return permissionVersion;
    }
}
