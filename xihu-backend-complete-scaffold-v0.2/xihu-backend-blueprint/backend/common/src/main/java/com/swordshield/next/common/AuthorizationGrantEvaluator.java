package com.swordshield.next.common;
import java.util.Collection;
/** Fail-closed, allow-only grant evaluator; deny overrides/inheritance not yet implemented. */
public final class AuthorizationGrantEvaluator {
    public boolean permits(AuthorizationRequest request, Collection<AuthorizationGrant> grants) {
        return grants != null && grants.stream().anyMatch(g -> g.permits(request));
    }
}
