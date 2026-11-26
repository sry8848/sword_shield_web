package com.swordshield.next.identity.infrastructure;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.swordshield.next.identity.application.PortalAuthentication;
import com.swordshield.next.identity.application.PortalAuthentication.NewSession;

@TableName("identity_session")
class IdentitySession {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private final String accountId;
    private final String clientType;
    private final int credentialVersion;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastUsedAt;
    private final LocalDateTime idleExpiresAt;
    private final LocalDateTime absoluteExpiresAt;

    /** 将新建 Portal 会话的时间统一转换为数据库使用的 UTC。 */
    IdentitySession(NewSession session) {
        accountId = session.accountId();
        clientType = PortalAuthentication.CLIENT;
        credentialVersion = session.credentialVersion();
        createdAt = LocalDateTime.ofInstant(session.createdAt(), ZoneOffset.UTC);
        lastUsedAt = createdAt;
        idleExpiresAt = LocalDateTime.ofInstant(session.idleExpiresAt(), ZoneOffset.UTC);
        absoluteExpiresAt = LocalDateTime.ofInstant(session.absoluteExpiresAt(), ZoneOffset.UTC);
    }

    String getId() {
        return id;
    }

    LocalDateTime getCreatedAt() {
        return createdAt;
    }

    LocalDateTime getIdleExpiresAt() {
        return idleExpiresAt;
    }
}
