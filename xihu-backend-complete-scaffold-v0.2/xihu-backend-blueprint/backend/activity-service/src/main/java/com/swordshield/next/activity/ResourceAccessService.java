package com.swordshield.next.activity;
import com.swordshield.next.common.ResourceRef;
import com.swordshield.next.common.ScopeDecision;
import com.swordshield.next.common.ScopeEvaluator;
import com.swordshield.next.common.ScopeKind;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
/** Demo authorization adapter: in production read role/organization scopes from authoritative stores,
 * not long-lived JWT claims. Missing or malformed scope = deny. */
@Service
public class ResourceAccessService {
    private final ScopeEvaluator evaluator = new ScopeEvaluator();
    public void require(Jwt jwt, String permission, String resourceType, String resourceId) {
        if (jwt == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if (jwt.getClaimAsStringList("permissions") == null || !jwt.getClaimAsStringList("permissions").contains(permission))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Missing function permission");
        Map<String,Object> policies = jwt.getClaimAsMap("scope_policies");
        if (policies == null || !(policies.get(permission) instanceof String kind))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Missing scope policy");
        try {
            ScopeKind scopeKind = ScopeKind.valueOf(kind);
            ScopeDecision decision = new ScopeDecision(permission, resourceType, scopeKind, Set.of());
            if (!evaluator.permits(jwt.getSubject(),permission,new ResourceRef(resourceType,resourceId),Set.of(decision)))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Resource not in allowed scope");
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Unsupported scope policy",e);
        }
    }
}
