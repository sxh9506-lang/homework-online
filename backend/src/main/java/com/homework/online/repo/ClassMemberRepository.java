package com.homework.online.repo;

import com.homework.online.domain.ClassMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassMemberRepository extends JpaRepository<ClassMember, Long> {

    List<ClassMember> findBySchoolClassIdOrderByIdAsc(Long classId);

    List<ClassMember> findByStudentId(Long studentId);

    List<ClassMember> findBySchoolClassIdIn(List<Long> classIds);

    Optional<ClassMember> findBySchoolClassIdAndStudentId(Long classId, Long studentId);

    boolean existsBySchoolClassIdAndStudentId(Long classId, Long studentId);

    long countBySchoolClassId(Long classId);

    void deleteBySchoolClassIdAndStudentId(Long classId, Long studentId);

    void deleteBySchoolClassId(Long classId);
}
