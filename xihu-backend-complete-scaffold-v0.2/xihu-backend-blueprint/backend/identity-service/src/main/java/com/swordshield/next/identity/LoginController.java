package com.swordshield.next.identity;
import com.swordshield.next.common.ApiEnvelope;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController
public class LoginController {
    private final JwtEncoder encoder;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    // Local-demo account ONLY. Replace with account+tenant_member+role tables for any actual deployment.
    private final String demoPasswordHash = passwordEncoder.encode("DemoPass123!");
    public LoginController(JwtEncoder encoder) { this.encoder = encoder; }
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    @PostMapping("/api/v1/auth/login")
    public ApiEnvelope<Map<String,Object>> login(@RequestBody @Valid LoginRequest request) {
        if (!"admin".equals(request.username()) || !passwordEncoder.matches(request.password(), demoPasswordHash))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid credentials");
        Instant now=Instant.now();
        List<String> permissions=List.of("event:read","event:create","event:publish","user:read");
        JwtClaimsSet claims=JwtClaimsSet.builder().issuer("sword-shield-demo")
          .issuedAt(now).expiresAt(now.plusSeconds(900)).subject("demo-admin")
          .claim("tenant_id","default").claim("roles",List.of("SUPER_ADMIN"))
          .claim("permissions",permissions)
          .claim("scope_policies", Map.of("event:read","ALL", "event:publish","ALL", "user:read","SELF")).build();
        String token=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
        return ApiEnvelope.ok(Map.of("accessToken",token,"tokenType","Bearer","expiresIn",900));
    }
    @GetMapping("/api/v1/admin/me")
    public ApiEnvelope<Map<String,Object>> me(@org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt) {
        return ApiEnvelope.ok(Map.of("user",Map.of("id",jwt.getSubject(),"displayName","演示管理员"),
          "roles",jwt.getClaimAsStringList("roles"),"permissions",jwt.getClaimAsStringList("permissions")));
    }
}
