package com.swordshield.next.common;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
public record ApiFailure(String code, String message, Map<String,Object> details, String traceId, String timestamp) {
    public static ApiFailure of(String code,String message) {
        String trace=MDC.get("traceId");
        return new ApiFailure(code,message,Map.of(),trace==null?UUID.randomUUID().toString():trace,Instant.now().toString());
    }
}
