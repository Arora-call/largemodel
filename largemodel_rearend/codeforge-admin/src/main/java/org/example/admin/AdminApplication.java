/**
 * CodeForge Admin Service — port 8085
 * 功能：管理后台 — 用户管理、模型配置、系统日志、统计
 */
package org.example.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "org.example")
public class AdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
