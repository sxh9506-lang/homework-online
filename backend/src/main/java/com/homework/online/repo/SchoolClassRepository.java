package com.homework.online.repo;

import com.homework.online.domain.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {

    List<SchoolClass> findByTeacherIdOrderByIdDesc(Long teacherId);

    Optional<SchoolClass> findByCode(String code);

    boolean existsByCode(String code);

    List<SchoolClass> findByIdInOrderByIdDesc(List<Long> ids);
}
