package com.swordshield.next.activity;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;
import com.swordshield.next.common.ApiFailure;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
@Configuration @EnableWebSecurity
public class SecurityConfig {
    @Bean JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String value) {
        if (value.getBytes(StandardCharsets.UTF_8).length < 32) throw new IllegalArgumentException("JWT secret needs >=32 bytes");
        SecretKey key=new SecretKeySpec(value.getBytes(StandardCharsets.UTF_8),"HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }
    @Bean JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter=new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<String> permissions=jwt.getClaimAsStringList("permissions");
            if (permissions==null) return List.of();
            return permissions.stream().map(p -> (GrantedAuthority) new SimpleGrantedAuthority(p)).toList();
        });
        return converter;
    }
    @Bean SecurityFilterChain security(HttpSecurity http,JwtAuthenticationConverter converter,ObjectMapper mapper) throws Exception {
        return http.exceptionHandling(e->e
           .authenticationEntryPoint((req,res,ex)->writeError(res,mapper,401,"UNAUTHENTICATED","未登录或凭证无效"))
           .accessDeniedHandler((req,res,ex)->writeError(res,mapper,403,"FORBIDDEN","没有执行该操作的权限")))
          .csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(a->a.requestMatchers("/api/v1/public/**","/actuator/health").permitAll()
             .anyRequest().authenticated())
          .oauth2ResourceServer(o->o.jwt(j->j.jwtAuthenticationConverter(converter))).build();
    }
    private static void writeError(jakarta.servlet.http.HttpServletResponse response,ObjectMapper mapper,int status,String code,String msg) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        mapper.writeValue(response.getOutputStream(),ApiFailure.of(code,msg));
    }
}
