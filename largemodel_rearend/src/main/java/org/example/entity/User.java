/**
 * 模块：用户体系
 * 功能：用户实体，映射users表，包含用户名、密码、角色、状态等核心字段
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 */
package org.example.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.enums.UserRole;
import org.example.enums.UserStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 200)
    private String password;

    @Column(length = 100)
    private String nickname;

    @Column(length = 150)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 500)
    private String avatar;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Column(nullable = false)
    @Builder.Default
    private Integer status = UserStatus.ENABLED.getCode();

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    public boolean isAdmin() {
        return UserRole.ADMIN.equals(this.role);
    }

    public boolean isEnabled() {
        return UserStatus.ENABLED.getCode() == this.status;
    }
}
