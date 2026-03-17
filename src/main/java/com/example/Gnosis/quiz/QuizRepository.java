package com.example.Gnosis.quiz;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
	List<Quiz> findByProfessorIdOrderByCreatedAtDesc(String professorId);
}
