package com.swordshield.next.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

@Component
final class InternalEndpoints implements WebFilter {

    private static final PathPattern INTERNAL =
            PathPatternParser.defaultInstance.parse("/internal/**");

    /** 公共入口始终隐藏服务间调用路径。 */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (INTERNAL.matches(exchange.getRequest().getPath().pathWithinApplication())) {
            return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
        }
        return chain.filter(exchange);
    }
}
