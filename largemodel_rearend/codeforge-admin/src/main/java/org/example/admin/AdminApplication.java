/**
 * CodeForge Admin Service — port 8085
 * 功能：管理后台 — 用户管理、模型配置、系统日志、统计
 */
package org.example.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = { "org.example.admin", "org.example.mapper", "org.example.config", "org.example.service" })
@EnableJpaRepositories("org.example.repository")
@EntityScan("org.example.entity")
public class AdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
