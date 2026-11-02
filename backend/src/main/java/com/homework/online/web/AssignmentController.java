package com.homework.online.web;

import com.homework.online.common.ApiException;
import com.homework.online.common.AuthUser;
import com.homework.online.common.CurrentUser;
import com.homework.online.dto.AssignmentDtos;
import com.homework.online.dto.SubmissionDtos;
import com.homework.online.service.AssignmentService;
import com.homework.online.service.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final SubmissionService submissionService;

    public AssignmentController(AssignmentService assignmentService, SubmissionService submissionService) {
        this.assignmentService = assignmentService;
        this.submissionService = submissionService;
    }

    @GetMapping
    public List<AssignmentDtos.AssignmentDto> list(@RequestParam(required = false) Long classId,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String keyword) {
        return assignmentService.list(CurrentUser.get(), classId, status, keyword);
    }

    @PostMapping
    public AssignmentDtos.AssignmentDto create(@Valid @RequestBody AssignmentDtos.SaveAssignmentRequest request) {
        CurrentUser.requireTeacher();
        return assignmentService.create(CurrentUser.get(), request);
    }

    @GetMapping("/{id}")
    public AssignmentDtos.AssignmentDto detail(@PathVariable Long id) {
        return assignmentService.detail(id, CurrentUser.get());
    }

    @PutMapping("/{id}")
    public AssignmentDtos.AssignmentDto update(@PathVariable Long id,
                                               @Valid @RequestBody AssignmentDtos.SaveAssignmentRequest request) {
        CurrentUser.requireTeacher();
        return assignmentService.update(id, CurrentUser.get(), request);
    }

    @PostMapping("/{id}/publish")
    public AssignmentDtos.AssignmentDto publish(@PathVariable Long id) {
        CurrentUser.requireTeacher();
        return assignmentService.publish(id, CurrentUser.get());
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        CurrentUser.requireTeacher();
        assignmentService.delete(id, CurrentUser.get());
        return Map.of("ok", true);
    }

    /** 教师：班级每位学生的提交情况 */
    @GetMapping("/{id}/submissions")
    public List<SubmissionDtos.StudentSubmissionRow> submissions(@PathVariable Long id) {
        CurrentUser.requireTeacher();
        AuthUser viewer = CurrentUser.get();
        return assignmentService.studentRows(assignmentService.requireOwnedAssignment(id, viewer), viewer);
    }

    /** 学生：提交 / 重新提交作业 */
    @PostMapping("/{id}/submissions")
    public SubmissionDtos.SubmissionDto submit(@PathVariable Long id,
                                               @RequestBody SubmissionDtos.SubmitRequest request) {
        if (!CurrentUser.get().isStudent()) {
            throw ApiException.forbidden("仅学生可提交作业");
        }
        return submissionService.submit(id, CurrentUser.get(), request);
    }
}
