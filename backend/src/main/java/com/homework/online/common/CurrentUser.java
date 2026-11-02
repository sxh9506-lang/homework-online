package com.homework.online.common;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** 从 SecurityContext 读取当前登录用户 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static AuthUser get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUser authUser) {
            return authUser;
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录或登录已过期");
    }

    public static void requireTeacher() {
        if (!get().isTeacher()) {
            throw ApiException.forbidden("仅教师可执行该操作");
        }
    }

    public static void requireStudent() {
        if (!get().isStudent()) {
            throw ApiException.forbidden("仅学生可执行该操作");
        }
    }
}
