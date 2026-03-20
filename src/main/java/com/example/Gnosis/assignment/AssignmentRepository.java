package com.example.Gnosis.assignment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
	List<Assignment> findByProfessorIdOrderByCreatedAtDesc(String professorId);
	List<Assignment> findAllByOrderByCreatedAtDesc();
	long countByProfessorId(String professorId);
}
