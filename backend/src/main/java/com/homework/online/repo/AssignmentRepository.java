package com.homework.online.repo;

import com.homework.online.domain.Assignment;
import com.homework.online.domain.AssignmentStatus;
import com.homework.online.domain.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findBySchoolClassIdOrderByCreatedAtDesc(Long classId);

    List<Assignment> findBySchoolClassIdInOrderByCreatedAtDesc(Collection<Long> classIds);

    List<Assignment> findBySchoolClassIdInAndStatusOrderByCreatedAtDesc(Collection<Long> classIds, AssignmentStatus status);

    List<Assignment> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

    List<Assignment> findByTeacherIdAndStatusOrderByCreatedAtDesc(Long teacherId, AssignmentStatus status);

    boolean existsBySchoolClass(SchoolClass schoolClass);

    List<Assignment> findBySchoolClassId(Long classId);
}
