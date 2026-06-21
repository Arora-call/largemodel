package org.example.admin.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan({ "org.example.mapper", "org.example.admin.mapper" })
public class AdminConfig {
}
