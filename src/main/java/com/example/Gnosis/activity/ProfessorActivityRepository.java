package com.example.Gnosis.activity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProfessorActivityRepository extends JpaRepository<ProfessorActivity, Long> {
	List<ProfessorActivity> findByProfessorIdOrderByCreatedAtDesc(String professorId);
}
