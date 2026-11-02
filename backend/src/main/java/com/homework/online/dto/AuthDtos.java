package com.homework.online.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 认证相关 DTO */
public final class AuthDtos {

    private AuthDtos() {
    }

    public static class LoginRequest {
        @NotBlank(message = "请输入用户名")
        public String username;

        @NotBlank(message = "请输入密码")
        public String password;
    }

    public static class RegisterRequest {
        @NotBlank(message = "请输入用户名")
        @Size(min = 3, max = 20, message = "用户名需为 3-20 个字符")
        public String username;

        @NotBlank(message = "请输入密码")
        @Size(min = 6, max = 40, message = "密码至少 6 位")
        public String password;

        @NotBlank(message = "请输入姓名")
        public String name;

        public String studentNo;
        public String classCode;
    }

    public static class ProfileRequest {
        public String name;
        public String email;
    }

    public static class UserDto {
        public Long id;
        public String username;
        public String name;
        public String role;
        public String studentNo;
        public String email;
        public String subject;
    }

    public static class AuthResponse {
        public String token;
        public UserDto user;

        public AuthResponse(String token, UserDto user) {
            this.token = token;
            this.user = user;
        }
    }
}
