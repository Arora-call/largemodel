package org.example.auth.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan({ "org.example.mapper", "org.example.auth.mapper" })
public class AuthConfig {
}
