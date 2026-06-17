/**
 * 模块：用户体系
 * 功能：用户角色枚举，定义USER和ADMIN两种角色
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 */
package org.example.enums;

public enum UserRole {
    USER("普通用户"),
    ADMIN("管理员");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
