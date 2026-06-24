/**
 * CodeForge Agent Service — port 8084
 * 功能：多 Agent 工作流编排、任务调度、SSE 流式执行
 */
package org.example.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = { "org.example.agent", "org.example.mapper", "org.example.config", "org.example.service.ai" })
public class AgentApplication {
    public static void main(String[] args) {
        System.setProperty("nacos.logging.default.config.enabled", "false");
        SpringApplication.run(AgentApplication.class, args);
    }
}
