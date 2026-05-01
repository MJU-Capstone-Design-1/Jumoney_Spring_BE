package com.mju.Jumoney.domain.user.enums;

public enum Role {
    USER("ROLE_USER", "일반 사용자"),
    ADMIN("ROLE_ADMIN", "관리자");

    private final String key;
    private final String description;

    Role(String key, String description) {
        this.key = key;
        this.description = description;
    }

    public String getKey() {
        return key;
    }
    public String getDescription() {
        return description;
    }
}
