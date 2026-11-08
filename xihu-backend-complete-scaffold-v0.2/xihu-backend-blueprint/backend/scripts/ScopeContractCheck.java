import com.swordshield.next.common.*;
import java.util.Set;
public class ScopeContractCheck {
    public static void main(String[] args) {
        ScopeEvaluator e = new ScopeEvaluator();
        ScopeDecision self = new ScopeDecision("user:read", "user", ScopeKind.SELF, Set.of());
        ScopeDecision assigned = new ScopeDecision("checkin:create", "event", ScopeKind.ASSIGNED, Set.of("E01", "E02"));
        if (!e.permits("u1", "user:read", new ResourceRef("user","u1"), Set.of(self))) throw new AssertionError("self denied");
        if (e.permits("u1", "user:read", new ResourceRef("user","u2"), Set.of(self))) throw new AssertionError("other user allowed");
        if (!e.permits("u1", "checkin:create", new ResourceRef("event","E02"), Set.of(assigned))) throw new AssertionError("assigned denied");
        if (e.permits("u1", "checkin:create", new ResourceRef("event","E03"), Set.of(assigned))) throw new AssertionError("unassigned allowed");
        if (e.permits("u1", "checkin:create", new ResourceRef("user","E02"), Set.of(assigned))) throw new AssertionError("wrong resource type allowed");
        System.out.println("PASS: self / assigned / resource-type separation");
    }
}
