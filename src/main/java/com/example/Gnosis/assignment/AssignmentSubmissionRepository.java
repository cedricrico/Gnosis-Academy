package com.example.Gnosis.assignment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {
	List<AssignmentSubmission> findByProfessorIdOrderBySubmittedAtDesc(String professorId);
	List<AssignmentSubmission> findByStudentIdOrderBySubmittedAtDesc(String studentId);
	java.util.Optional<AssignmentSubmission> findByAssignmentIdAndStudentId(Long assignmentId, String studentId);
	boolean existsByAssignmentIdAndStudentId(Long assignmentId, String studentId);
	long countByAssignmentId(Long assignmentId);
}
