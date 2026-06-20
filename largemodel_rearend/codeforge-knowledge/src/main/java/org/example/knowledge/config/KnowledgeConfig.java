/**
 * 知识库模块配置
 */
package org.example.knowledge.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan({ "org.example.knowledge.mapper", "org.example.mapper" })
public class KnowledgeConfig {
}
