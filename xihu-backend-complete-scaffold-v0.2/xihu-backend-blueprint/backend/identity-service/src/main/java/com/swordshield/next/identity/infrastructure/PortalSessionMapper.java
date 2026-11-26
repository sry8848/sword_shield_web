package com.swordshield.next.identity.infrastructure;

import java.time.LocalDateTime;
import java.util.Optional;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
interface PortalSessionMapper extends BaseMapper<IdentitySession> {
    /** 根据凭证摘要锁定所属 Portal 会话并返回其 ID。 */
    Optional<String> lockSession(@Param("hash") byte[] tokenHash);

    /** 读取已锁定会话的账号、凭证状态和数据库时间。 */
    LockedRefresh selectRefreshState(@Param("id") String sessionId, @Param("hash") byte[] tokenHash);

    void insertRefreshToken(@Param("id") String sessionId, @Param("hash") byte[] tokenHash,
            @Param("issuedAt") LocalDateTime issuedAt, @Param("expiresAt") LocalDateTime expiresAt);

    void consumeRefreshToken(@Param("hash") byte[] tokenHash);

    void updateLastUse(@Param("id") String sessionId, @Param("lastUsedAt") LocalDateTime lastUsedAt,
            @Param("idleDeadline") LocalDateTime idleDeadline);

    int revokeActiveSession(@Param("id") String sessionId);

    void insertReplayAudit(@Param("sessionId") String sessionId, @Param("accountId") String accountId,
            @Param("traceId") String traceId);

    record LockedRefresh(String accountId, String phone, boolean enabled, int accountVersion,
            int sessionVersion, int permissionVersion, boolean revoked, LocalDateTime now,
            LocalDateTime idleExpiresAt, LocalDateTime absoluteExpiresAt, LocalDateTime tokenExpiresAt,
            LocalDateTime consumedAt) {
    }
}
