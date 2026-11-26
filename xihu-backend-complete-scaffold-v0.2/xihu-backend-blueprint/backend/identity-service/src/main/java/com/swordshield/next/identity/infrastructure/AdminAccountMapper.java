package com.swordshield.next.identity.infrastructure;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
interface AdminAccountMapper {
    /** 一次读取账号凭证和当前固定后台角色。 */
    List<LoginRow> selectLogin(@Param("phone") String phone);

    /** 一次读取会话对应账号的当前状态和固定后台角色。 */
    List<SessionRow> selectSession(@Param("id") String id);

    record LoginRow(String id, String passwordHash, boolean enabled, int credentialVersion, String roleCode) {
    }

    record SessionRow(String phone, boolean enabled, int credentialVersion, String roleCode) {
    }
}
