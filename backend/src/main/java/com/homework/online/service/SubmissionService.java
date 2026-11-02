package com.homework.online.service;

import com.homework.online.common.ApiException;
import com.homework.online.common.AuthUser;
import com.homework.online.domain.Assignment;
import com.homework.online.domain.AssignmentStatus;
import com.homework.online.domain.StoredFile;
import com.homework.online.domain.Submission;
import com.homework.online.domain.SubmissionStatus;
import com.homework.online.domain.User;
import com.homework.online.dto.SubmissionDtos;
import com.homework.online.repo.AssignmentRepository;
import com.homework.online.repo.ClassMemberRepository;
import com.homework.online.repo.SubmissionRepository;
import com.homework.online.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final ClassMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final AssignmentService assignmentService;
    private final Mapper mapper;

    public SubmissionService(SubmissionRepository submissionRepository,
                             AssignmentRepository assignmentRepository,
                             ClassMemberRepository memberRepository,
                             UserRepository userRepository,
                             AssignmentService assignmentService,
                             Mapper mapper) {
        this.submissionRepository = submissionRepository;
        this.assignmentRepository = assignmentRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.assignmentService = assignmentService;
        this.mapper = mapper;
    }

    /* ------------------------------ 学生提交 ------------------------------ */

    @Transactional
    public SubmissionDtos.SubmissionDto submit(Long assignmentId, AuthUser viewer, SubmissionDtos.SubmitRequest request) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> ApiException.notFound("作业不存在"));
        if (assignment.getStatus() != AssignmentStatus.PUBLISHED) {
            throw ApiException.badRequest("该作业尚未发布");
        }
        if (!memberRepository.existsBySchoolClassIdAndStudentId(assignment.getSchoolClass().getId(), viewer.id())) {
            throw ApiException.forbidden("你不在此班级中");
        }

        String content = request.content == null ? "" : request.content.trim();
        List<StoredFile> attachments = assignmentService.loadFiles(request.attachments);
        if (content.isEmpty() && attachments.isEmpty()) {
            throw ApiException.badRequest("请先填写作业内容或上传附件");
        }

        LocalDateTime now = LocalDateTime.now();
        boolean late = assignment.getDueAt() != null && now.isAfter(assignment.getDueAt());
        if (late && !assignment.isAllowLate()) {
            throw ApiException.badRequest("已超过截止时间，该作业不允许补交");
        }

        Submission submission = submissionRepository
                .findByAssignmentIdAndStudentId(assignmentId, viewer.id())
                .orElseGet(() -> {
                    Submission created = new Submission();
                    created.setAssignment(assignment);
                    created.setStudent(userRepository.findById(viewer.id())
                            .orElseThrow(() -> ApiException.notFound("用户不存在")));
                    return created;
                });
        if (submission.getStatus() == SubmissionStatus.GRADED) {
            throw ApiException.badRequest("该作业已批改，如需重交请联系老师");
        }
        submission.setContent(content);
        submission.setAttachments(attachments);
        submission.setSubmittedAt(now);
        submission.setStatus(SubmissionStatus.SUBMITTED);
        return mapper.toSubmissionDto(submissionRepository.save(submission));
    }

    public List<SubmissionDtos.SubmissionDto> mine(AuthUser viewer) {
        List<SubmissionDtos.SubmissionDto> result = new ArrayList<>();
        for (Submission submission : submissionRepository.findByStudentIdOrderBySubmittedAtDesc(viewer.id())) {
            result.add(mapper.toSubmissionDto(submission));
        }
        result.sort(Comparator.comparing((SubmissionDtos.SubmissionDto d) -> d.submittedAt,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return result;
    }

    /* ------------------------------ 教师批改 ------------------------------ */

    public List<SubmissionDtos.SubmissionDto> listForTeacher(AuthUser viewer, String status, Long assignmentId,
                                                             Long classId, String keyword) {
        List<Submission> submissions = assignmentId != null
                ? submissionRepository.findByAssignmentIdOrderBySubmittedAtDesc(assignmentId)
                : submissionRepository.findByAssignmentTeacherIdOrderBySubmittedAtDesc(viewer.id());

        List<SubmissionDtos.SubmissionDto> result = new ArrayList<>();
        String normalized = status == null ? "ALL" : status.trim().toUpperCase();
        String kw = keyword == null ? "" : keyword.trim();
        for (Submission submission : submissions) {
            Assignment assignment = submission.getAssignment();
            if (assignment == null || assignment.getTeacher() == null
                    || !assignment.getTeacher().getId().equals(viewer.id())) {
                continue;
            }
            if (!"ALL".equals(normalized) && !submission.getStatus().name().equals(normalized)) {
                continue;
            }
            if (classId != null && !assignment.getSchoolClass().getId().equals(classId)) {
                continue;
            }
            if (!kw.isEmpty()) {
                String name = submission.getStudent() == null ? "" : submission.getStudent().getName();
                String no = submission.getStudent() == null || submission.getStudent().getStudentNo() == null
                        ? "" : submission.getStudent().getStudentNo();
                if (!name.contains(kw) && !no.contains(kw) && !(assignment.getTitle() != null && assignment.getTitle().contains(kw))) {
                    continue;
                }
            }
            result.add(mapper.toSubmissionDto(submission));
        }
        result.sort(Comparator.comparing((SubmissionDtos.SubmissionDto d) -> d.submittedAt,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return result;
    }

    public SubmissionDtos.SubmissionDto detail(Long id, AuthUser viewer) {
        Submission submission = requireVisible(id, viewer);
        return mapper.toSubmissionDto(submission);
    }

    @Transactional
    public SubmissionDtos.SubmissionDto grade(Long id, AuthUser viewer, SubmissionDtos.GradeRequest request) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("提交记录不存在"));
        Assignment assignment = submission.getAssignment();
        if (assignment == null || assignment.getTeacher() == null
                || !assignment.getTeacher().getId().equals(viewer.id())) {
            throw ApiException.forbidden("无权批改该作业");
        }
        if (request.score == null) {
            throw ApiException.badRequest("请填写分数");
        }
        if (request.score < 0 || request.score > assignment.getTotalScore()) {
            throw ApiException.badRequest("分数需在 0 ~ " + assignment.getTotalScore() + " 之间");
        }
        submission.setScore(request.score);
        submission.setComment(request.comment == null ? "" : request.comment);
        submission.setStatus("RETURNED".equalsIgnoreCase(request.status)
                ? SubmissionStatus.RETURNED : SubmissionStatus.GRADED);
        submission.setGradedAt(LocalDateTime.now());
        submission.setGradedBy(userRepository.findById(viewer.id()).orElse(null));
        return mapper.toSubmissionDto(submissionRepository.save(submission));
    }

    @Transactional
    public void withdraw(Long id, AuthUser viewer) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("提交记录不存在"));
        if (!viewer.isStudent() || submission.getStudent() == null
                || !submission.getStudent().getId().equals(viewer.id())
                || submission.getStatus() == SubmissionStatus.GRADED) {
            throw ApiException.forbidden("无权撤回该提交");
        }
        submissionRepository.delete(submission);
    }

    public List<SubmissionDtos.StudentSubmissionRow> rowsForAssignment(Assignment assignment, AuthUser viewer) {
        return assignmentService.studentRows(assignment, viewer);
    }

    private Submission requireVisible(Long id, AuthUser viewer) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("提交记录不存在"));
        Assignment assignment = submission.getAssignment();
        if (viewer.isTeacher()) {
            if (assignment == null || assignment.getTeacher() == null
                    || !assignment.getTeacher().getId().equals(viewer.id())) {
                throw ApiException.forbidden("无权查看该提交");
            }
        } else if (submission.getStudent() == null || !submission.getStudent().getId().equals(viewer.id())) {
            throw ApiException.forbidden("无权查看该提交");
        }
        return submission;
    }

    /** 供工作台统计使用 */
    public List<Submission> findByTeacher(Long teacherId) {
        return submissionRepository.findByAssignmentTeacherIdOrderBySubmittedAtDesc(teacherId);
    }

    public User studentOf(Submission submission) {
        return submission.getStudent();
    }
}
