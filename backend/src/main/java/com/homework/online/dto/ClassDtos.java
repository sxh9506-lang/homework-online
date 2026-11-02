package com.homework.online.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.List;

/** 班级相关 DTO */
public final class ClassDtos {

    private ClassDtos() {
    }

    public static class ClassDto {
        public Long id;
        public String name;
        public String code;
        public Long teacherId;
        public String teacherName;
        public long studentCount;
        public LocalDateTime createdAt;
    }

    public static class ClassDetailDto {
        public Long id;
        public String name;
        public String code;
        public Long teacherId;
        public String teacherName;
        public long studentCount;
        public LocalDateTime createdAt;
        public List<AuthDtos.UserDto> students;
        public List<AssignmentDtos.AssignmentDto> assignments;
    }

    public static class CreateClassRequest {
        @NotBlank(message = "班级名称不能为空")
        public String name;

        public String code;
    }

    public static class RenameClassRequest {
        @NotBlank(message = "班级名称不能为空")
        public String name;
    }

    public static class JoinClassRequest {
        @NotBlank(message = "请输入班级邀请码")
        public String code;
    }

    public static class AddStudentRequest {
        @NotBlank(message = "请输入学生用户名或学号")
        public String username;
    }
}
