package com.example.Gnosis.schoolclass;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {
	List<SchoolClass> findAllByOrderByCreatedAtDesc();
}
