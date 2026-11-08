package com.swordshield.next.common;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class ScopeEvaluatorTest {
  private final ScopeEvaluator evaluator=new ScopeEvaluator();
  @Test void userSelfOnly() {
    ScopeDecision grant=new ScopeDecision("user:read","user",ScopeKind.SELF,Set.of());
    assertTrue(evaluator.permits("alice","user:read",new ResourceRef("user","alice"),Set.of(grant)));
    assertFalse(evaluator.permits("alice","user:read",new ResourceRef("user","bob"),Set.of(grant)));
  }
  @Test void assignedEventsNeverLeakToUserScope() {
    ScopeDecision grant=new ScopeDecision("checkin:create","event",ScopeKind.ASSIGNED,Set.of("E01"));
    assertTrue(evaluator.permits("alice","checkin:create",new ResourceRef("event","E01"),Set.of(grant)));
    assertFalse(evaluator.permits("alice","checkin:create",new ResourceRef("event","E02"),Set.of(grant)));
    assertFalse(evaluator.permits("alice","checkin:create",new ResourceRef("user","E01"),Set.of(grant)));
    assertFalse(evaluator.permits("alice","event:update",new ResourceRef("event","E01"),Set.of(grant)));
  }
  @Test void denyWithoutGrants() {
    assertFalse(evaluator.permits("alice","user:read",new ResourceRef("user","alice"),Set.of()));
  }
}
