package com.homework.online.repo;

import com.homework.online.domain.Submission;
import com.homework.online.domain.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByAssignmentIdOrderBySubmittedAtDesc(Long assignmentId);

    Optional<Submission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    List<Submission> findByStudentIdOrderBySubmittedAtDesc(Long studentId);

    List<Submission> findByAssignmentTeacherIdOrderBySubmittedAtDesc(Long teacherId);

    List<Submission> findByAssignmentTeacherIdAndStatusOrderBySubmittedAtDesc(Long teacherId, SubmissionStatus status);

    long countByAssignmentId(Long assignmentId);

    void deleteByAssignmentId(Long assignmentId);
}
