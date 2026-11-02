package com.homework.online.service;

import com.homework.online.common.ApiException;
import com.homework.online.common.AuthUser;
import com.homework.online.domain.Assignment;
import com.homework.online.domain.AssignmentStatus;
import com.homework.online.domain.ClassMember;
import com.homework.online.domain.SchoolClass;
import com.homework.online.domain.StoredFile;
import com.homework.online.domain.Submission;
import com.homework.online.domain.SubmissionStatus;
import com.homework.online.dto.AssignmentDtos;
import com.homework.online.repo.AssignmentRepository;
import com.homework.online.repo.ClassMemberRepository;
import com.homework.online.repo.SchoolClassRepository;
import com.homework.online.repo.StoredFileRepository;
import com.homework.online.repo.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ClassMemberRepository memberRepository;
    private final SubmissionRepository submissionRepository;
    private final StoredFileRepository fileRepository;
    private final Mapper mapper;

    public AssignmentService(AssignmentRepository assignmentRepository,
                             SchoolClassRepository schoolClassRepository,
                             ClassMemberRepository memberRepository,
                             SubmissionRepository submissionRepository,
                             StoredFileRepository fileRepository,
                             Mapper mapper) {
        this.assignmentRepository = assignmentRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.memberRepository = memberRepository;
        this.submissionRepository = submissionRepository;
        this.fileRepository = fileRepository;
        this.mapper = mapper;
    }

    /* ------------------------------ 查询 ------------------------------ */

    public List<AssignmentDtos.AssignmentDto> list(AuthUser viewer, Long classId, String status, String keyword) {
        List<Assignment> assignments;
        if (viewer.isTeacher()) {
            if (classId != null) {
                SchoolClass schoolClass = requireOwnedClass(classId, viewer);
                assignments = new ArrayList<>(assignmentRepository
                        .findBySchoolClassIdOrderByCreatedAtDesc(schoolClass.getId()));
            } else {
                assignments = new ArrayList<>(assignmentRepository
                        .findByTeacherIdOrderByCreatedAtDesc(viewer.id()));
            }
        } else {
            List<Long> classIds = classIdsOfStudent(viewer.id());
            if (classIds.isEmpty()) {
                return new ArrayList<>();
            }
            assignments = new ArrayList<>(assignmentRepository
                    .findBySchoolClassIdInAndStatusOrderByCreatedAtDesc(classIds, AssignmentStatus.PUBLISHED));
            if (classId != null) {
                assignments.removeIf(a -> !a.getSchoolClass().getId().equals(classId));
            }
        }

        String normalizedStatus = status == null ? "ALL" : status.trim().toUpperCase();
        if ("DRAFT".equals(normalizedStatus) || "PUBLISHED".equals(normalizedStatus)) {
            assignments.removeIf(a -> !a.getStatus().name().equals(normalizedStatus));
        }
        if ("PENDING".equals(normalizedStatus)) {
            assignments.removeIf(a -> submissionRepository
                    .findByAssignmentIdAndStudentId(a.getId(), viewer.id()).isPresent());
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            assignments.removeIf(a -> !(a.getTitle() != null && a.getTitle().contains(kw))
                    && !(a.getContent() != null && a.getContent().contains(kw)));
        }

        assignments.sort(Comparator.comparing(Assignment::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        List<AssignmentDtos.AssignmentDto> result = new ArrayList<>();
        for (Assignment assignment : assignments) {
            result.add(toDto(assignment, viewer));
        }
        return result;
    }

    public AssignmentDtos.AssignmentDto detail(Long id, AuthUser viewer) {
        Assignment assignment = requireVisible(id, viewer);
        AssignmentDtos.AssignmentDto dto = toDto(assignment, viewer);
        if (viewer.isTeacher()) {
            dto.submissions = studentRows(assignment, viewer);
        }
        return dto;
    }

    /** 教师视角：班级每位学生的提交情况 */
    public List<com.homework.online.dto.SubmissionDtos.StudentSubmissionRow> studentRows(Assignment assignment, AuthUser viewer) {
        Map<Long, Submission> byStudent = new LinkedHashMap<>();
        for (Submission submission : submissionRepository.findByAssignmentIdOrderBySubmittedAtDesc(assignment.getId())) {
            byStudent.putIfAbsent(submission.getStudent().getId(), submission);
        }
        List<com.homework.online.dto.SubmissionDtos.StudentSubmissionRow> rows = new ArrayList<>();
        for (ClassMember member : memberRepository.findBySchoolClassIdOrderByIdAsc(assignment.getSchoolClass().getId())) {
            var student = member.getStudent();
            var row = new com.homework.online.dto.SubmissionDtos.StudentSubmissionRow();
            row.studentId = student.getId();
            row.studentName = student.getName();
            row.studentNo = student.getStudentNo();
            Submission submission = byStudent.get(student.getId());
            row.submission = submission == null ? null : mapper.toSubmissionDto(submission);
            rows.add(row);
        }
        return rows;
    }

    /* ------------------------------ 编辑 ------------------------------ */

    @Transactional
    public AssignmentDtos.AssignmentDto create(AuthUser viewer, AssignmentDtos.SaveAssignmentRequest request) {
        SchoolClass schoolClass = requireOwnedClass(request.classId, viewer);
        Assignment assignment = new Assignment();
        assignment.setTeacher(schoolClass.getTeacher());
        assignment.setSchoolClass(schoolClass);
        apply(assignment, request, schoolClass);
        assignment.setCreatedAt(LocalDateTime.now());
        if (assignment.getStatus() == AssignmentStatus.PUBLISHED) {
            assignment.setPublishedAt(LocalDateTime.now());
        }
        return toDto(assignmentRepository.save(assignment), viewer);
    }

    @Transactional
    public AssignmentDtos.AssignmentDto update(Long id, AuthUser viewer, AssignmentDtos.SaveAssignmentRequest request) {
        Assignment assignment = requireOwnedAssignment(id, viewer);
        SchoolClass schoolClass = requireOwnedClass(request.classId == null ? assignment.getSchoolClass().getId() : request.classId, viewer);
        assignment.setSchoolClass(schoolClass);
        apply(assignment, request, schoolClass);
        return toDto(assignmentRepository.save(assignment), viewer);
    }

    @Transactional
    public AssignmentDtos.AssignmentDto publish(Long id, AuthUser viewer) {
        Assignment assignment = requireOwnedAssignment(id, viewer);
        assignment.setStatus(AssignmentStatus.PUBLISHED);
        if (assignment.getPublishedAt() == null) {
            assignment.setPublishedAt(LocalDateTime.now());
        }
        return toDto(assignmentRepository.save(assignment), viewer);
    }

    @Transactional
    public void delete(Long id, AuthUser viewer) {
        Assignment assignment = requireOwnedAssignment(id, viewer);
        submissionRepository.deleteByAssignmentId(assignment.getId());
        assignmentRepository.delete(assignment);
    }

    private void apply(Assignment assignment, AssignmentDtos.SaveAssignmentRequest request, SchoolClass schoolClass) {
        assignment.setTitle(request.title.trim());
        assignment.setContent(request.content);
        assignment.setDueAt(request.dueAt);
        assignment.setTotalScore(request.totalScore == null || request.totalScore <= 0 ? 100 : request.totalScore);
        assignment.setAllowLate(request.allowLate == null || request.allowLate);
        if ("PUBLISHED".equalsIgnoreCase(request.status)) {
            assignment.setStatus(AssignmentStatus.PUBLISHED);
            if (assignment.getPublishedAt() == null) {
                assignment.setPublishedAt(LocalDateTime.now());
            }
        } else if ("CLOSED".equalsIgnoreCase(request.status)) {
            assignment.setStatus(AssignmentStatus.CLOSED);
        } else if (request.status != null) {
            assignment.setStatus(AssignmentStatus.DRAFT);
        }
        assignment.setAttachments(loadFiles(request.attachments));
        assignment.setSchoolClass(schoolClass);
    }

    /* ------------------------------ 转换 ------------------------------ */

    public AssignmentDtos.AssignmentDto toDto(Assignment assignment, AuthUser viewer) {
        AssignmentDtos.AssignmentDto dto = new AssignmentDtos.AssignmentDto();
        dto.id = assignment.getId();
        dto.title = assignment.getTitle();
        dto.content = assignment.getContent();
        SchoolClass schoolClass = assignment.getSchoolClass();
        dto.classId = schoolClass != null ? schoolClass.getId() : null;
        dto.className = schoolClass != null ? schoolClass.getName() : "";
        dto.teacherId = assignment.getTeacher() != null ? assignment.getTeacher().getId() : null;
        dto.teacherName = assignment.getTeacher() != null ? assignment.getTeacher().getName() : "";
        dto.dueAt = assignment.getDueAt();
        dto.totalScore = assignment.getTotalScore();
        dto.allowLate = assignment.isAllowLate();
        dto.status = assignment.getStatus().name();
        dto.attachments = mapper.toFileDtos(assignment.getAttachments());
        dto.createdAt = assignment.getCreatedAt();
        dto.publishedAt = assignment.getPublishedAt();
        dto.overdue = assignment.getDueAt() != null && assignment.getDueAt().isBefore(LocalDateTime.now());

        if (schoolClass != null) {
            dto.studentCount = memberRepository.countBySchoolClassId(schoolClass.getId());
        }
        List<Submission> submissions = submissionRepository.findByAssignmentIdOrderBySubmittedAtDesc(assignment.getId());
        dto.submittedCount = submissions.size();
        dto.gradedCount = submissions.stream()
                .filter(s -> s.getStatus() == SubmissionStatus.GRADED)
                .count();
        if (viewer != null && viewer.isStudent()) {
            submissions.stream()
                    .filter(s -> s.getStudent() != null && s.getStudent().getId().equals(viewer.id()))
                    .findFirst()
                    .ifPresent(s -> dto.mySubmission = mapper.toSubmissionDto(s));
        }
        return dto;
    }

    /* ------------------------------ 内部工具 ------------------------------ */

    public List<StoredFile> loadFiles(List<Long> ids) {
        List<StoredFile> files = new ArrayList<>();
        if (ids == null) {
            return files;
        }
        for (Long fileId : ids) {
            if (fileId != null) {
                fileRepository.findById(fileId).ifPresent(files::add);
            }
        }
        return files;
    }

    public SchoolClass requireOwnedClass(Long classId, AuthUser viewer) {
        SchoolClass schoolClass = schoolClassRepository.findById(classId)
                .orElseThrow(() -> ApiException.badRequest("班级不存在"));
        if (!schoolClass.getTeacher().getId().equals(viewer.id())) {
            throw ApiException.forbidden("只有班级创建者可以操作该班级");
        }
        return schoolClass;
    }

    public Assignment requireVisible(Long id, AuthUser viewer) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("作业不存在或无权查看"));
        if (viewer.isTeacher()) {
            if (assignment.getTeacher() == null || !assignment.getTeacher().getId().equals(viewer.id())) {
                throw ApiException.notFound("作业不存在或无权查看");
            }
        } else {
            boolean member = memberRepository
                    .existsBySchoolClassIdAndStudentId(assignment.getSchoolClass().getId(), viewer.id());
            if (!member || assignment.getStatus() != AssignmentStatus.PUBLISHED) {
                throw ApiException.notFound("作业不存在或无权查看");
            }
        }
        return assignment;
    }

    public Assignment requireOwnedAssignment(Long id, AuthUser viewer) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("作业不存在"));
        if (assignment.getTeacher() == null || !assignment.getTeacher().getId().equals(viewer.id())) {
            throw ApiException.notFound("作业不存在");
        }
        return assignment;
    }

    public List<Long> classIdsOfStudent(Long studentId) {
        List<Long> ids = new ArrayList<>();
        for (ClassMember member : memberRepository.findByStudentId(studentId)) {
            ids.add(member.getSchoolClass().getId());
        }
        return ids;
    }
}
