package com.homework.online.common;

/** 认证通过后放入 SecurityContext 的当前用户信息 */
public record AuthUser(Long id, String username, String role) {

    public boolean isTeacher() {
        return "TEACHER".equals(role);
    }

    public boolean isStudent() {
        return "STUDENT".equals(role);
    }
}
