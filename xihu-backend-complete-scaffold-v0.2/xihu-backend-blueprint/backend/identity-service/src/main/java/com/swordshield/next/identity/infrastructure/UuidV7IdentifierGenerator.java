package com.swordshield.next.identity.infrastructure;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.swordshield.next.common.UuidV7Ids;
import org.springframework.stereotype.Component;

@Component
class UuidV7IdentifierGenerator implements IdentifierGenerator {

    /** 实体使用 ASSIGN_UUID 插入时，生成可写入 CHAR(36) 的资源标识。 */
    @Override
    public String nextUUID(Object entity) {
        return UuidV7Ids.next();
    }

    /** 数字主键配置属于错误，立即终止写入。 */
    @Override
    public Number nextId(Object entity) {
        throw new IllegalStateException("业务主键必须使用 ASSIGN_UUID 生成 UUIDv7");
    }
}
