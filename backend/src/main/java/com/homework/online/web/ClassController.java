package com.homework.online.web;

import com.homework.online.common.CurrentUser;
import com.homework.online.dto.ClassDtos;
import com.homework.online.service.ClassService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/classes")
public class ClassController {

    private final ClassService classService;

    public ClassController(ClassService classService) {
        this.classService = classService;
    }

    @GetMapping
    public List<ClassDtos.ClassDto> list() {
        return classService.list(CurrentUser.get());
    }

    @PostMapping
    public ClassDtos.ClassDto create(@Valid @RequestBody ClassDtos.CreateClassRequest request) {
        CurrentUser.requireTeacher();
        return classService.create(CurrentUser.get(), request);
    }

    @GetMapping("/{id}")
    public ClassDtos.ClassDetailDto detail(@PathVariable Long id) {
        return classService.detail(id, CurrentUser.get());
    }

    @PutMapping("/{id}")
    public ClassDtos.ClassDto rename(@PathVariable Long id, @Valid @RequestBody ClassDtos.RenameClassRequest request) {
        CurrentUser.requireTeacher();
        return classService.rename(id, CurrentUser.get(), request);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        CurrentUser.requireTeacher();
        classService.delete(id, CurrentUser.get());
        return Map.of("ok", true);
    }

    @PostMapping("/join")
    public ClassDtos.ClassDto join(@Valid @RequestBody ClassDtos.JoinClassRequest request) {
        CurrentUser.requireStudent();
        return classService.join(CurrentUser.get(), request.code);
    }

    @PostMapping("/{id}/students")
    public ClassDtos.ClassDto addStudent(@PathVariable Long id, @Valid @RequestBody ClassDtos.AddStudentRequest request) {
        CurrentUser.requireTeacher();
        return classService.addStudent(id, CurrentUser.get(), request.username);
    }

    @DeleteMapping("/{id}/students/{studentId}")
    public Map<String, Object> removeStudent(@PathVariable Long id, @PathVariable Long studentId) {
        CurrentUser.requireTeacher();
        classService.removeStudent(id, studentId, CurrentUser.get());
        return Map.of("ok", true);
    }
}
