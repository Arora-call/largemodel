/**
 * Agent 模块配置
 */
package org.example.agent.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan({ "org.example.agent.mapper", "org.example.mapper" })
public class AgentConfig {
}
