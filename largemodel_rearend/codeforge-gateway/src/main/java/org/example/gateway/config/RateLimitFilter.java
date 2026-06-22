/**
 * 简易限流全局过滤器 — 基于 ConcurrentHashMap 滑动窗口
 * 无需 Redis/Sentinel，重启后计数归零
 */
package org.example.gateway.config;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    /** 每个路径前缀的每秒最大请求数（按 IP） */
    private static final Map<String, Integer> LIMITS = Map.of(
            "/api/ai/generate/stream", 5,
            "/api/ai/modify/stream", 10,
            "/api/ai/review", 10,
            "/api/projects/generate", 5,
            "/api/auth/login", 20
    );

    private final ConcurrentHashMap<String, long[]> counters = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String matched = LIMITS.keySet().stream().filter(path::startsWith).findFirst().orElse(null);
        if (matched == null) return chain.filter(exchange);

        long maxQps = LIMITS.get(matched);
        String ip = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown";
        String key = ip + ":" + matched;
        long now = Instant.now().getEpochSecond();

        long[] slot = counters.computeIfAbsent(key, k -> new long[] { now, 0 });
        synchronized (slot) {
            if (slot[0] != now) { slot[0] = now; slot[1] = 0; }
            if (slot[1] >= maxQps) {
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                return exchange.getResponse().setComplete();
            }
            slot[1]++;
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() { return -1; } // 高优先级
}
