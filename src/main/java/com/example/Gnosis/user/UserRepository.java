package com.example.Gnosis.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByStudentId(String studentId);
	boolean existsByStudentId(String studentId);

	List<User> findByCourseIgnoreCaseAndSectionNameIgnoreCaseOrderByLastNameAscFirstNameAsc(String course, String sectionName);
	List<User> findBySectionNameIgnoreCaseOrderByLastNameAscFirstNameAsc(String sectionName);
	List<User> findByCourseIgnoreCaseContainingOrderByLastNameAscFirstNameAsc(String course);
}
