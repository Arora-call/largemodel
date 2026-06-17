/**
 * 模块：系统初始化
 * 功能：数据初始化器，应用启动时自动创建默认角色和管理员账号
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 */
package org.example.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.enums.UserRole;
import org.example.enums.UserStatus;
import org.example.repository.RoleRepository;
import org.example.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        // 初始化角色
        initRoles();

        // 初始化管理员账号
        initAdminUser();
    }

    private void initRoles() {
        if (roleRepository.count() == 0) {
            Role userRole = Role.builder()
                    .name(UserRole.USER.name())
                    .description(UserRole.USER.getDisplayName())
                    .build();
            roleRepository.save(userRole);

            Role adminRole = Role.builder()
                    .name(UserRole.ADMIN.name())
                    .description(UserRole.ADMIN.getDisplayName())
                    .build();
            roleRepository.save(adminRole);

            log.info("默认角色已初始化");
        }
    }

    private void initAdminUser() {
        if (!userRepository.existsByUsernameAndDeletedFalse("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .nickname("系统管理员")
                    .email("admin@example.com")
                    .role(UserRole.ADMIN)
                    .status(UserStatus.ENABLED.getCode())
                    .deleted(false)
                    .build();

            admin.setCreatedBy("system");
            userRepository.save(admin);
            log.info("默认管理员账号已创建: admin / admin123");
        }
    }
}
