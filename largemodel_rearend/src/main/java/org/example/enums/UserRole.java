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
