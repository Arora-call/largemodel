/**
 * 模块：数据访问
 * 功能：MyBatis-Plus 配置 — MapperScan
 * 注意：分页插件由 Spring Boot 自动配置，无需手动注册
 * 作者：yx
 * 创建时间：2026-06-20
 */
package org.example.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("org.example.mapper")
public class MyBatisPlusConfig {
}
