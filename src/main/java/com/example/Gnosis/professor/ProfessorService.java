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
			String professorId,
			String firstName,
			String middleInitial,
			String lastName,
			Integer age,
			String sex,
			String department,
			String position,
			String password,
			String confirmPassword
	) {
		if (!password.equals(confirmPassword)) {
			throw new IllegalArgumentException("Passwords do not match");
		}
		if (professorRepository.existsByProfessorId(professorId)) {
			throw new IllegalArgumentException("Professor ID already exists");
		}

		String passwordHash = passwordEncoder.encode(password);
		Professor professor = new Professor(
				professorId,
				firstName,
				emptyToNull(middleInitial),
				lastName,
				age,
				sex,
				department,
				position,
				passwordHash
		);
		return professorRepository.save(professor);
	}

	@Transactional(readOnly = true)
	public Optional<Professor> authenticate(String professorId, String rawPassword) {
		return professorRepository.findByProfessorId(professorId)
				.filter(professor -> passwordEncoder.matches(rawPassword, professor.getPasswordHash()));
	}

	private static String emptyToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
