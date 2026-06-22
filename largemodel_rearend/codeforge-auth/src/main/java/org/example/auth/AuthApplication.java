/**
 * CodeForge Auth Service — port 8081
 * 功能：用户注册/登录、JWT 签发、个人信息管理、管理员用户管理
 */
package org.example.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = { "org.example" })
@EnableJpaRepositories("org.example.repository")
@EntityScan("org.example.entity")
public class AuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
