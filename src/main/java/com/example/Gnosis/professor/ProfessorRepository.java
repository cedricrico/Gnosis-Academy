package com.example.Gnosis.professor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfessorRepository extends JpaRepository<Professor, Long> {
	Optional<Professor> findByProfessorId(String professorId);
	boolean existsByProfessorId(String professorId);
}
