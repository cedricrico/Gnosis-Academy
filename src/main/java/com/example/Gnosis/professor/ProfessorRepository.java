package com.example.Gnosis.professor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfessorRepository extends JpaRepository<Professor, Long> {
	Optional<Professor> findByEmployeeId(String employeeId);
	Optional<Professor> findByEmail(String email);
	boolean existsByEmployeeId(String employeeId);
	boolean existsByEmail(String email);
}
