package com.homework.online.web;

import com.homework.online.common.CurrentUser;
import com.homework.online.dto.SubmissionDtos;
import com.homework.online.service.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    /** 学生：我的提交记录 */
    @GetMapping("/mine")
    public List<SubmissionDtos.SubmissionDto> mine() {
        CurrentUser.requireStudent();
        return submissionService.mine(CurrentUser.get());
    }

    /** 教师：待批改 / 已批改列表 */
    @GetMapping
    public List<SubmissionDtos.SubmissionDto> list(@RequestParam(required = false) String status,
                                                   @RequestParam(required = false) Long assignmentId,
                                                   @RequestParam(required = false) Long classId,
                                                   @RequestParam(required = false) String keyword) {
        CurrentUser.requireTeacher();
        return submissionService.listForTeacher(CurrentUser.get(), status, assignmentId, classId, keyword);
    }

    @GetMapping("/{id}")
    public SubmissionDtos.SubmissionDto detail(@PathVariable Long id) {
        return submissionService.detail(id, CurrentUser.get());
    }

    /** 教师批改 */
    @PostMapping("/{id}/grade")
    public SubmissionDtos.SubmissionDto grade(@PathVariable Long id,
                                              @Valid @RequestBody SubmissionDtos.GradeRequest request) {
        CurrentUser.requireTeacher();
        return submissionService.grade(id, CurrentUser.get(), request);
    }

    /** 学生撤回未批改的提交 */
    @DeleteMapping("/{id}")
    public Map<String, Object> withdraw(@PathVariable Long id) {
        submissionService.withdraw(id, CurrentUser.get());
        return Map.of("ok", true);
    }
}
