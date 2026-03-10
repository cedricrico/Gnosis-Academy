package com.example.Gnosis.professor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ProfessorService {
	private final ProfessorRepository professorRepository;
	private final PasswordEncoder passwordEncoder;

	public ProfessorService(ProfessorRepository professorRepository, PasswordEncoder passwordEncoder) {
		this.professorRepository = professorRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public Professor register(
			String employeeId,
			String firstName,
			String middleInitial,
			String lastName,
			String email,
			String department,
			String password,
			String confirmPassword
	) {
		String normalizedEmployeeId = requireValue(employeeId, "Employee ID is required");
		String normalizedFirstName = requireValue(firstName, "First name is required");
		String normalizedLastName = requireValue(lastName, "Last name is required");
		String normalizedEmail = requireValue(email, "Email is required");
		String normalizedDepartment = requireValue(department, "Department is required");

		if (!password.equals(confirmPassword)) {
			throw new IllegalArgumentException("Passwords do not match");
		}
		if (professorRepository.existsByEmployeeId(normalizedEmployeeId)) {
			throw new IllegalArgumentException("Employee ID already exists");
		}
		if (professorRepository.existsByEmail(normalizedEmail)) {
			throw new IllegalArgumentException("Email already exists");
		}

		String passwordHash = passwordEncoder.encode(password);
		Professor professor = new Professor(
				normalizedEmployeeId,
				normalizedFirstName,
				emptyToNull(middleInitial),
				normalizedLastName,
				normalizedDepartment,
				normalizedEmail,
				passwordHash
		);
		return professorRepository.save(professor);
	}

	@Transactional(readOnly = true)
	public Optional<Professor> authenticate(String employeeId, String rawPassword) {
		return professorRepository.findByEmployeeId(employeeId)
				.filter(professor -> passwordEncoder.matches(rawPassword, professor.getPasswordHash()));
	}

	private static String emptyToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static String requireValue(String value, String message) {
		String trimmed = emptyToNull(value);
		if (trimmed == null) {
			throw new IllegalArgumentException(message);
		}
		return trimmed;
	}
}
