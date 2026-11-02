package com.homework.online.service;

import com.homework.online.common.AuthUser;
import com.homework.online.domain.Assignment;
import com.homework.online.domain.AssignmentStatus;
import com.homework.online.domain.ClassMember;
import com.homework.online.domain.SchoolClass;
import com.homework.online.domain.Submission;
import com.homework.online.domain.SubmissionStatus;
import com.homework.online.domain.User;
import com.homework.online.dto.StatsDtos;
import com.homework.online.repo.AssignmentRepository;
import com.homework.online.repo.ClassMemberRepository;
import com.homework.online.repo.SchoolClassRepository;
import com.homework.online.repo.SubmissionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class StatsService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SchoolClassRepository schoolClassRepository;
    private final ClassMemberRepository memberRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;

    public StatsService(SchoolClassRepository schoolClassRepository,
                        ClassMemberRepository memberRepository,
                        AssignmentRepository assignmentRepository,
                        SubmissionRepository submissionRepository) {
        this.schoolClassRepository = schoolClassRepository;
        this.memberRepository = memberRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
    }

    public StatsDtos.OverviewResponse overview(AuthUser viewer) {
        return viewer.isTeacher() ? teacherOverview(viewer) : studentOverview(viewer);
    }

    private StatsDtos.OverviewResponse teacherOverview(AuthUser viewer) {
        List<SchoolClass> classes = schoolClassRepository.findByTeacherIdOrderByIdDesc(viewer.id());
        List<Long> classIds = new ArrayList<>();
        for (SchoolClass schoolClass : classes) {
            classIds.add(schoolClass.getId());
        }
        Set<Long> studentIds = new HashSet<>();
        if (!classIds.isEmpty()) {
            for (ClassMember member : memberRepository.findBySchoolClassIdIn(classIds)) {
                studentIds.add(member.getStudent().getId());
            }
        }
        List<Assignment> assignments = assignmentRepository.findByTeacherIdOrderByCreatedAtDesc(viewer.id());
        List<Submission> submissions = submissionRepository.findByAssignmentTeacherIdOrderBySubmittedAtDesc(viewer.id());

        List<Submission> pending = new ArrayList<>();
        List<Submission> graded = new ArrayList<>();
        for (Submission submission : submissions) {
            if (submission.getStatus() == SubmissionStatus.SUBMITTED) {
                pending.add(submission);
            } else if (submission.getStatus() == SubmissionStatus.GRADED) {
                graded.add(submission);
            }
        }
        BigDecimal average = BigDecimal.ZERO;
        if (!graded.isEmpty()) {
            int sum = graded.stream().mapToInt(s -> s.getScore() == null ? 0 : s.getScore()).sum();
            average = BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(graded.size()), 1, RoundingMode.HALF_UP);
        }

        StatsDtos.OverviewResponse response = new StatsDtos.OverviewResponse();
        response.role = "TEACHER";
        response.cards = new ArrayList<>();
        response.cards.add(new StatsDtos.StatCard("我的班级", String.valueOf(classes.size()), "个", "class", false));
        response.cards.add(new StatsDtos.StatCard("学生人数", String.valueOf(studentIds.size()), "人", "student", false));
        response.cards.add(new StatsDtos.StatCard("待批改作业", String.valueOf(pending.size()), "份", "pen", !pending.isEmpty()));
        response.cards.add(new StatsDtos.StatCard("平均得分", graded.isEmpty() ? "—" : average.toPlainString(),
                graded.isEmpty() ? "" : "分", "chart", false));

        pending.sort(Comparator.comparing(Submission::getSubmittedAt));
        response.todo = new ArrayList<>();
        for (Submission submission : pending) {
            if (response.todo.size() >= 6) {
                break;
            }
            Assignment assignment = submission.getAssignment();
            User student = submission.getStudent();
            StatsDtos.TodoItem item = new StatsDtos.TodoItem();
            item.kind = "SUBMISSION";
            item.id = submission.getId();
            item.assignmentId = assignment != null ? assignment.getId() : null;
            item.title = assignment != null ? assignment.getTitle() : "";
            item.subtitle = (student == null ? "" : student.getName() + " · "
                    + (student.getStudentNo() == null ? "" : student.getStudentNo()));
            item.meta = "提交于 " + (submission.getSubmittedAt() == null ? "" : submission.getSubmittedAt().format(TIME_FORMAT));
            item.submittedAt = submission.getSubmittedAt();
            item.late = assignment != null && assignment.getDueAt() != null && submission.getSubmittedAt() != null
                    && submission.getSubmittedAt().isAfter(assignment.getDueAt());
            response.todo.add(item);
        }

        response.recent = new ArrayList<>();
        List<Assignment> recentAssignments = new ArrayList<>(assignments);
        recentAssignments.sort(Comparator.comparing(Assignment::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        for (Assignment assignment : recentAssignments) {
            if (response.recent.size() >= 5) {
                break;
            }
            long submitted = submissionRepository.countByAssignmentId(assignment.getId());
            long gradedCount = submissionRepository.findByAssignmentIdOrderBySubmittedAtDesc(assignment.getId()).stream()
                    .filter(s -> s.getStatus() == SubmissionStatus.GRADED).count();
            StatsDtos.RecentItem item = new StatsDtos.RecentItem();
            item.kind = "ASSIGNMENT";
            item.id = assignment.getId();
            item.assignmentId = assignment.getId();
            item.title = assignment.getTitle();
            item.subtitle = assignment.getSchoolClass() != null ? assignment.getSchoolClass().getName() : "";
            item.meta = "已交 " + submitted + "/" + memberRepository.countBySchoolClassId(assignment.getSchoolClass().getId())
                    + " · 已批 " + gradedCount;
            item.status = assignment.getStatus().name();
            item.dueAt = assignment.getDueAt();
            response.recent.add(item);
        }
        return response;
    }

    private StatsDtos.OverviewResponse studentOverview(AuthUser viewer) {
        List<Long> classIds = new ArrayList<>();
        List<SchoolClass> classes = new ArrayList<>();
        for (ClassMember member : memberRepository.findByStudentId(viewer.id())) {
            classIds.add(member.getSchoolClass().getId());
            classes.add(member.getSchoolClass());
        }
        List<Assignment> published = classIds.isEmpty() ? new ArrayList<>()
                : assignmentRepository.findBySchoolClassIdInAndStatusOrderByCreatedAtDesc(classIds, AssignmentStatus.PUBLISHED);
        List<Submission> mine = submissionRepository.findByStudentIdOrderBySubmittedAtDesc(viewer.id());

        List<Assignment> pendingAssignments = new ArrayList<>();
        for (Assignment assignment : published) {
            boolean submitted = mine.stream().anyMatch(s -> s.getAssignment() != null
                    && s.getAssignment().getId().equals(assignment.getId()));
            if (!submitted) {
                pendingAssignments.add(assignment);
            }
        }
        pendingAssignments.sort(Comparator.comparing(Assignment::getDueAt,
                Comparator.nullsLast(Comparator.naturalOrder())));

        List<Submission> graded = new ArrayList<>();
        for (Submission submission : mine) {
            if (submission.getStatus() == SubmissionStatus.GRADED) {
                graded.add(submission);
            }
        }
        double rate = 0;
        if (!graded.isEmpty()) {
            double sum = 0;
            for (Submission submission : graded) {
                int total = submission.getAssignment() != null && submission.getAssignment().getTotalScore() != null
                        ? submission.getAssignment().getTotalScore() : 100;
                sum += (submission.getScore() == null ? 0 : submission.getScore()) * 100.0 / Math.max(1, total);
            }
            rate = sum / graded.size();
        }

        StatsDtos.OverviewResponse response = new StatsDtos.OverviewResponse();
        response.role = "STUDENT";
        response.cards = new ArrayList<>();
        response.cards.add(new StatsDtos.StatCard("我的班级", String.valueOf(classes.size()), "个", "class", false));
        response.cards.add(new StatsDtos.StatCard("待完成作业", String.valueOf(pendingAssignments.size()), "项", "todo",
                !pendingAssignments.isEmpty()));
        response.cards.add(new StatsDtos.StatCard("已提交", String.valueOf(mine.size()), "份", "upload", false));
        response.cards.add(new StatsDtos.StatCard("平均得分率", graded.isEmpty() ? "—" : String.valueOf(Math.round(rate)),
                graded.isEmpty() ? "" : "%", "chart", false));

        response.todo = new ArrayList<>();
        for (Assignment assignment : pendingAssignments) {
            StatsDtos.TodoItem item = new StatsDtos.TodoItem();
            item.kind = "ASSIGNMENT";
            item.id = assignment.getId();
            item.assignmentId = assignment.getId();
            item.title = assignment.getTitle();
            item.subtitle = assignment.getSchoolClass() != null ? assignment.getSchoolClass().getName() : "";
            item.meta = assignment.getDueAt() == null ? "不限时" : "截止 " + assignment.getDueAt().format(TIME_FORMAT);
            item.dueAt = assignment.getDueAt();
            item.overdue = assignment.getDueAt() != null && assignment.getDueAt().isBefore(LocalDateTime.now());
            response.todo.add(item);
        }

        response.recent = new ArrayList<>();
        for (Submission submission : mine) {
            if (response.recent.size() >= 5) {
                break;
            }
            Assignment assignment = submission.getAssignment();
            StatsDtos.RecentItem item = new StatsDtos.RecentItem();
            item.kind = "SUBMISSION";
            item.id = submission.getId();
            item.assignmentId = assignment != null ? assignment.getId() : null;
            item.title = assignment != null ? assignment.getTitle() : "";
            item.status = submission.getStatus().name();
            if (submission.getStatus() == SubmissionStatus.GRADED) {
                item.subtitle = "已批改";
                item.meta = submission.getScore() + " / " + (assignment != null ? assignment.getTotalScore() : 100) + " 分";
                item.gradedAt = submission.getGradedAt();
            } else {
                item.subtitle = "等待批改";
                item.meta = "已提交";
            }
            response.recent.add(item);
        }
        return response;
    }
}
