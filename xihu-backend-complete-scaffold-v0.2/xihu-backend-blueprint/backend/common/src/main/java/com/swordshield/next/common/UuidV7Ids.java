package com.swordshield.next.common;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochRandomGenerator;

public final class UuidV7Ids {

    // 同一毫秒内的每次生成都重新填充随机部分。
    private static final TimeBasedEpochRandomGenerator GENERATOR = Generators.timeBasedEpochRandomGenerator();

    private UuidV7Ids() {
    }

    /** 生成标准小写、带连字符的 UUIDv7 业务资源标识。 */
    public static String next() {
        return GENERATOR.generate().toString();
    }
}
