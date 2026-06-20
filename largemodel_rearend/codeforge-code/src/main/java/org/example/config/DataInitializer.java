/**
 * 模块：系统初始化
 * 功能：启动时自动创建默认管理员账号
 * 作者：yx
 * 创建时间：2026-06-17
 */
package org.example.config;

import lombok.extern.slf4j.Slf4j;
import org.example.entity.User;
import org.example.enums.UserRole;
import org.example.enums.UserStatus;
import org.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String defaultPassword;

    public DataInitializer(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.admin.default-password:admin123}") String defaultPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.defaultPassword = defaultPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByUsernameAndDeletedFalse("admin")) return;

        User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode(defaultPassword))
                .nickname("系统管理员")
                .email("admin@example.com")
                .role(UserRole.ADMIN)
                .status(UserStatus.ENABLED.getCode())
                .deleted(false)
                .build();
        admin.setCreatedBy("system");
        userRepository.save(admin);
        log.info("默认管理员账号已创建: admin / {}", defaultPassword);
        if ("admin123".equals(defaultPassword)) {
            log.warn("⚠ 正在使用默认密码，请在生产环境中通过 app.admin.default-password 配置修改");
        }
    }
}
