/**
 * CodeForge API Gateway
 * 功能：统一入口、路由转发、JWT 验证、CORS、限流
 */
package org.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        System.setProperty("nacos.logging.default.config.enabled", "false");
        SpringApplication.run(GatewayApplication.class, args);
    }
}
