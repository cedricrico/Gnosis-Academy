package com.example.Gnosis.assignment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {
	List<AssignmentSubmission> findByProfessorIdOrderBySubmittedAtDesc(String professorId);
}
