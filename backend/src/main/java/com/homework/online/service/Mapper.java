package com.homework.online.service;

import com.homework.online.domain.Assignment;
import com.homework.online.domain.StoredFile;
import com.homework.online.domain.Submission;
import com.homework.online.domain.User;
import com.homework.online.dto.AuthDtos;
import com.homework.online.dto.FileDto;
import com.homework.online.dto.SubmissionDtos;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** 实体 -> DTO 的转换 */
@Component
public class Mapper {

    public AuthDtos.UserDto toUserDto(User user) {
        if (user == null) {
            return null;
        }
        AuthDtos.UserDto dto = new AuthDtos.UserDto();
        dto.id = user.getId();
        dto.username = user.getUsername();
        dto.name = user.getName();
        dto.role = user.getRole().name();
        dto.studentNo = user.getStudentNo();
        dto.email = user.getEmail();
        dto.subject = user.getSubject();
        return dto;
    }

    public FileDto toFileDto(StoredFile file) {
        if (file == null) {
            return null;
        }
        return new FileDto(file.getId(), file.getName(), file.getSize(), file.getContentType());
    }

    public List<FileDto> toFileDtos(List<StoredFile> files) {
        List<FileDto> list = new ArrayList<>();
        if (files != null) {
            for (StoredFile file : files) {
                list.add(toFileDto(file));
            }
        }
        return list;
    }

    public SubmissionDtos.SubmissionDto toSubmissionDto(Submission submission) {
        if (submission == null) {
            return null;
        }
        Assignment assignment = submission.getAssignment();
        SubmissionDtos.SubmissionDto dto = new SubmissionDtos.SubmissionDto();
        dto.id = submission.getId();
        dto.assignmentId = assignment != null ? assignment.getId() : null;
        dto.assignmentTitle = assignment != null ? assignment.getTitle() : null;
        dto.totalScore = assignment != null ? assignment.getTotalScore() : null;
        dto.dueAt = assignment != null ? assignment.getDueAt() : null;
        dto.className = assignment != null && assignment.getSchoolClass() != null
                ? assignment.getSchoolClass().getName() : null;
        User student = submission.getStudent();
        dto.studentId = student != null ? student.getId() : null;
        dto.studentName = student != null ? student.getName() : "";
        dto.studentNo = student != null ? student.getStudentNo() : "";
        dto.content = submission.getContent();
        dto.attachments = toFileDtos(submission.getAttachments());
        dto.submittedAt = submission.getSubmittedAt();
        dto.status = submission.getStatus().name();
        dto.score = submission.getScore();
        dto.comment = submission.getComment();
        dto.gradedAt = submission.getGradedAt();
        dto.late = assignment != null && assignment.getDueAt() != null
                && submission.getSubmittedAt() != null
                && submission.getSubmittedAt().isAfter(assignment.getDueAt());
        return dto;
    }
}
