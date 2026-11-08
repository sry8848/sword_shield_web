import com.swordshield.next.common.*;
import java.util.*;
public class ExtendedAuthorizationCheck {
 public static void main(String[] args) {
  var evaluator = new AuthorizationGrantEvaluator();
  var grants = List.of(new AuthorizationGrant("tenant-a","user:read","user",ScopeKind.SELF,Set.of()),
      new AuthorizationGrant("tenant-a","checkin:create","event",ScopeKind.ASSIGNED,Set.of("E01","E02")));
  require(evaluator.permits(new AuthorizationRequest("U1","tenant-a","user:read","user","U2","U1"),grants),"SELF owner of user detail");
  require(!evaluator.permits(new AuthorizationRequest("U1","tenant-a","user:read","user","U2","U2"),grants),"no other owner");
  require(evaluator.permits(new AuthorizationRequest("U1","tenant-a","checkin:create","event","E02",null),grants),"assigned event");
  require(!evaluator.permits(new AuthorizationRequest("U1","tenant-a","checkin:create","event","E03",null),grants),"not assigned event");
  require(!evaluator.permits(new AuthorizationRequest("U1","tenant-b","checkin:create","event","E02",null),grants),"cross tenant blocked");
  require(!evaluator.permits(new AuthorizationRequest("U1","tenant-a","user:read","event","E02",null),grants),"resource type blocked");
  System.out.println("ExtendedAuthorizationCheck PASS: 6 assertions");
 }
 static void require(boolean ok,String name){if(!ok)throw new AssertionError(name);}
}
