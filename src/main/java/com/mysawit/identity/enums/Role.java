package com.mysawit.identity.enums;

import lombok.Getter;

@Getter
public enum Role {
    BURUH("BURUH"),
    MANDOR("MANDOR"),
    SUPIR("SUPIR"),
    ADMIN("ADMIN");

    private final String value;

    private Role(String value) {
        this.value = value;
    }

    public static boolean contains(String param) {
        for (Role role : Role.values()) {
            if (role.name().equals(param)) {
                return true;
            }
        }
        return false;
    }
}