package com.example.Gnosis.auth;

import com.example.Gnosis.user.User;
import com.example.Gnosis.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public User register(RegisterRequest request) {
		if (!request.getPassword().equals(request.getConfirmPassword())) {
			throw new IllegalArgumentException("Passwords do not match");
		}
		if (userRepository.existsByStudentId(request.getStudentId())) {
			throw new IllegalArgumentException("Student ID already exists");
		}

		String passwordHash = passwordEncoder.encode(request.getPassword());
		User user = new User(
				request.getStudentId(),
				request.getFirstName(),
				emptyToNull(request.getMiddleInitial()),
				request.getLastName(),
				request.getAge(),
				request.getSex(),
				passwordHash
		);
		// Self-registered students should start unassigned.
		user.setCourse("");
		user.setSectionName("");
		user.setStatus("Active");

		User saved = userRepository.save(user);

		// Guard against legacy DB defaults/triggers that may auto-assign sections on insert.
		if (saved.getSectionName() != null && !saved.getSectionName().isBlank()) {
			saved.setSectionName("");
			saved.setCourse("");
			saved = userRepository.save(saved);
		}

		return saved;
	}

	private static String emptyToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}

