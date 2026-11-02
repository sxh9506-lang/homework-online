package com.homework.online.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/** 作业相关 DTO */
public final class AssignmentDtos {

    private AssignmentDtos() {
    }

    public static class AssignmentDto {
        public Long id;
        public String title;
        public String content;
        public Long classId;
        public String className;
        public Long teacherId;
        public String teacherName;
        public LocalDateTime dueAt;
        public Integer totalScore;
        public Boolean allowLate;
        public String status;
        public List<FileDto> attachments;
        public LocalDateTime createdAt;
        public LocalDateTime publishedAt;
        public long studentCount;
        public long submittedCount;
        public long gradedCount;
        public boolean overdue;
        /** 学生视角：我自己的提交 */
        public SubmissionDtos.SubmissionDto mySubmission;
        /** 教师视角：学生提交情况 */
        public List<SubmissionDtos.StudentSubmissionRow> submissions;
    }

    public static class SaveAssignmentRequest {
        @NotBlank(message = "作业标题不能为空")
        public String title;

        @NotBlank(message = "作业内容不能为空")
        public String content;

        @NotNull(message = "请选择布置班级")
        public Long classId;

        public LocalDateTime dueAt;
        public Integer totalScore;
        public Boolean allowLate;
        /** DRAFT / PUBLISHED */
        public String status;
        public List<Long> attachments;
    }
}
