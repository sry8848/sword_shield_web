package com.swordshield.next.organization.domain.port;
import java.util.Optional;
import com.swordshield.next.organization.domain.model.OrgUnit;
/** 领域持久化端口；以后由该领域自己的 MyBatis adapter 实现，不得依赖其他服务的 Mapper。 */
public interface OrgUnitRepository {
    Optional<OrgUnit> findOwned(String tenantId, String resourceId);
}
