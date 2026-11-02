package com.homework.online.service;

import com.homework.online.common.ApiException;
import com.homework.online.common.AuthUser;
import com.homework.online.domain.Assignment;
import com.homework.online.domain.AssignmentStatus;
import com.homework.online.domain.ClassMember;
import com.homework.online.domain.Role;
import com.homework.online.domain.SchoolClass;
import com.homework.online.domain.User;
import com.homework.online.dto.ClassDtos;
import com.homework.online.repo.AssignmentRepository;
import com.homework.online.repo.ClassMemberRepository;
import com.homework.online.repo.SchoolClassRepository;
import com.homework.online.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Service
public class ClassService {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SchoolClassRepository schoolClassRepository;
    private final ClassMemberRepository memberRepository;
    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final AssignmentService assignmentService;
    private final Mapper mapper;

    public ClassService(SchoolClassRepository schoolClassRepository,
                        ClassMemberRepository memberRepository,
                        AssignmentRepository assignmentRepository,
                        UserRepository userRepository,
                        AssignmentService assignmentService,
                        Mapper mapper) {
        this.schoolClassRepository = schoolClassRepository;
        this.memberRepository = memberRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.assignmentService = assignmentService;
        this.mapper = mapper;
    }

    public List<ClassDtos.ClassDto> list(AuthUser viewer) {
        List<SchoolClass> classes;
        if (viewer.isTeacher()) {
            classes = schoolClassRepository.findByTeacherIdOrderByIdDesc(viewer.id());
        } else {
            List<Long> ids = new ArrayList<>();
            for (ClassMember member : memberRepository.findByStudentId(viewer.id())) {
                ids.add(member.getSchoolClass().getId());
            }
            classes = ids.isEmpty() ? new ArrayList<>() : schoolClassRepository.findByIdInOrderByIdDesc(ids);
        }
        List<ClassDtos.ClassDto> result = new ArrayList<>();
        for (SchoolClass schoolClass : classes) {
            result.add(toDto(schoolClass));
        }
        return result;
    }

    public ClassDtos.ClassDto toDto(SchoolClass schoolClass) {
        ClassDtos.ClassDto dto = new ClassDtos.ClassDto();
        dto.id = schoolClass.getId();
        dto.name = schoolClass.getName();
        dto.code = schoolClass.getCode();
        dto.teacherId = schoolClass.getTeacher() != null ? schoolClass.getTeacher().getId() : null;
        dto.teacherName = schoolClass.getTeacher() != null ? schoolClass.getTeacher().getName() : "";
        dto.studentCount = memberRepository.countBySchoolClassId(schoolClass.getId());
        dto.createdAt = schoolClass.getCreatedAt();
        return dto;
    }

    @Transactional
    public ClassDtos.ClassDto create(AuthUser viewer, ClassDtos.CreateClassRequest request) {
        User teacher = userRepository.findById(viewer.id())
                .orElseThrow(() -> ApiException.notFound("用户不存在"));
        String code = request.code == null || request.code.isBlank() ? generateCode() : request.code.trim().toUpperCase();
        if (schoolClassRepository.existsByCode(code)) {
            throw ApiException.badRequest("邀请码已被使用，请更换一个");
        }
        SchoolClass schoolClass = new SchoolClass(request.name.trim(), code, teacher);
        return toDto(schoolClassRepository.save(schoolClass));
    }

    public ClassDtos.ClassDetailDto detail(Long id, AuthUser viewer) {
        SchoolClass schoolClass = requireAccessible(id, viewer);

        ClassDtos.ClassDetailDto dto = new ClassDtos.ClassDetailDto();
        dto.id = schoolClass.getId();
        dto.name = schoolClass.getName();
        dto.code = schoolClass.getCode();
        dto.teacherId = schoolClass.getTeacher().getId();
        dto.teacherName = schoolClass.getTeacher().getName();
        dto.createdAt = schoolClass.getCreatedAt();

        dto.students = new ArrayList<>();
        for (ClassMember member : memberRepository.findBySchoolClassIdOrderByIdAsc(schoolClass.getId())) {
            dto.students.add(mapper.toUserDto(member.getStudent()));
        }
        dto.studentCount = dto.students.size();

        List<Assignment> assignments = assignmentRepository.findBySchoolClassIdOrderByCreatedAtDesc(schoolClass.getId());
        if (viewer.isStudent()) {
            assignments.removeIf(a -> a.getStatus() != AssignmentStatus.PUBLISHED);
        }
        dto.assignments = new ArrayList<>();
        for (Assignment assignment : assignments) {
            dto.assignments.add(assignmentService.toDto(assignment, viewer));
        }
        return dto;
    }

    @Transactional
    public ClassDtos.ClassDto rename(Long id, AuthUser viewer, ClassDtos.RenameClassRequest request) {
        SchoolClass schoolClass = requireOwned(id, viewer);
        schoolClass.setName(request.name.trim());
        return toDto(schoolClassRepository.save(schoolClass));
    }

    @Transactional
    public void delete(Long id, AuthUser viewer) {
        SchoolClass schoolClass = requireOwned(id, viewer);
        if (assignmentRepository.existsBySchoolClass(schoolClass)) {
            throw ApiException.badRequest("该班级下还有作业，无法删除");
        }
        memberRepository.deleteBySchoolClassId(schoolClass.getId());
        schoolClassRepository.delete(schoolClass);
    }

    @Transactional
    public ClassDtos.ClassDto join(AuthUser viewer, String code) {
        SchoolClass schoolClass = schoolClassRepository.findByCode(code.trim().toUpperCase())
                .orElseThrow(() -> ApiException.badRequest("邀请码不存在"));
        if (memberRepository.existsBySchoolClassIdAndStudentId(schoolClass.getId(), viewer.id())) {
            throw ApiException.badRequest("你已在该班级中");
        }
        User student = userRepository.findById(viewer.id())
                .orElseThrow(() -> ApiException.notFound("用户不存在"));
        memberRepository.save(new ClassMember(schoolClass, student));
        return toDto(schoolClass);
    }

    @Transactional
    public ClassDtos.ClassDto addStudent(Long classId, AuthUser viewer, String key) {
        SchoolClass schoolClass = requireOwned(classId, viewer);
        String value = key.trim();
        User student = userRepository.findByUsername(value).orElse(null);
        if (student == null) {
            student = userRepository.findByStudentNo(value).orElse(null);
        }
        if (student == null || student.getRole() != Role.STUDENT) {
            throw ApiException.badRequest("未找到该学生账号或学号");
        }
        if (memberRepository.existsBySchoolClassIdAndStudentId(schoolClass.getId(), student.getId())) {
            throw ApiException.badRequest("该学生已在班级中");
        }
        memberRepository.save(new ClassMember(schoolClass, student));
        return toDto(schoolClass);
    }

    @Transactional
    public void removeStudent(Long classId, Long studentId, AuthUser viewer) {
        SchoolClass schoolClass = requireOwned(classId, viewer);
        memberRepository.deleteBySchoolClassIdAndStudentId(schoolClass.getId(), studentId);
    }

    private String generateCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                builder.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            String code = builder.toString();
            if (!schoolClassRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new ApiException("邀请码生成失败，请重试");
    }

    private SchoolClass requireOwned(Long id, AuthUser viewer) {
        SchoolClass schoolClass = schoolClassRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("班级不存在"));
        if (!schoolClass.getTeacher().getId().equals(viewer.id())) {
            throw ApiException.forbidden("只有班级创建者可以操作该班级");
        }
        return schoolClass;
    }

    private SchoolClass requireAccessible(Long id, AuthUser viewer) {
        SchoolClass schoolClass = schoolClassRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("班级不存在"));
        if (viewer.isTeacher()) {
            if (!schoolClass.getTeacher().getId().equals(viewer.id())) {
                throw ApiException.forbidden("无权查看该班级");
            }
        } else if (!memberRepository.existsBySchoolClassIdAndStudentId(schoolClass.getId(), viewer.id())) {
            throw ApiException.forbidden("无权查看该班级");
        }
        return schoolClass;
    }
}
