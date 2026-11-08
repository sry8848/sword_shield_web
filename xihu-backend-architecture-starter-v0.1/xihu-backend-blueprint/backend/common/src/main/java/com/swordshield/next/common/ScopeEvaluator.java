package com.swordshield.next.common;
import java.util.Collection;
/** Pure policy evaluator; database-backed subject grants are resolved by an adapter. */
public final class ScopeEvaluator {
    public boolean permits(String actorId, String permission, ResourceRef resource, Collection<ScopeDecision> grants) {
        return grants.stream().anyMatch(g -> g.permissionCode().equals(permission) && g.allows(actorId, resource));
    }
}
