package com.swordshield.next.customerservice.application;
import com.swordshield.next.common.AuthorizationRequest;
/** 应用层未来实现用例 工单处理和转交。身份授权必须经统一契约，不能绕开服务端检查。 */
public interface ApplicationContract {
    /** First guard of a future command: validate current actor/scope/resource. */
    void requireAuthorization(AuthorizationRequest request);
}
