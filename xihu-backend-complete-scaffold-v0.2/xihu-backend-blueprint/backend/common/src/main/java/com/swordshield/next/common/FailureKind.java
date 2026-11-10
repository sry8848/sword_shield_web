package com.swordshield.next.common;

/** 业务失败的性质，与接口协议无关。 */
public enum FailureKind {
    /** 业务对象不存在。 */
    NOT_FOUND,
    /** 当前业务状态与操作冲突。 */
    CONFLICT,
    /** 输入格式合法，但不满足业务规则。 */
    RULE_VIOLATION
}
