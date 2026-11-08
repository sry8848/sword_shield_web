package com.swordshield.next.incentive.domain.port;
import java.util.Optional;
import com.swordshield.next.incentive.domain.model.PointLedgerEntry;
/** 领域持久化端口；以后由该领域自己的 MyBatis adapter 实现，不得依赖其他服务的 Mapper。 */
public interface PointLedgerRepository {
    Optional<PointLedgerEntry> findOwned(String tenantId, String resourceId);
}
