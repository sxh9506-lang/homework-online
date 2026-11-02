package com.homework.online.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/** 提交与批改相关 DTO */
public final class SubmissionDtos {

    private SubmissionDtos() {
    }

    public static class SubmissionDto {
        public Long id;
        public Long assignmentId;
        public String assignmentTitle;
        public Integer totalScore;
        public LocalDateTime dueAt;
        public String className;
        public Long studentId;
        public String studentName;
        public String studentNo;
        public String content;
        public List<FileDto> attachments;
        public LocalDateTime submittedAt;
        public String status;
        public Integer score;
        public String comment;
        public LocalDateTime gradedAt;
        public boolean late;
    }

    public static class StudentSubmissionRow {
        public Long studentId;
        public String studentName;
        public String studentNo;
        public SubmissionDto submission;
    }

    public static class SubmitRequest {
        public String content;
        public List<Long> attachments;
    }

    public static class GradeRequest {
        @NotNull(message = "请填写分数")
        public Integer score;

        public String comment;
        /** GRADED / RETURNED */
        public String status;
    }
}
