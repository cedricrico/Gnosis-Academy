package com.example.Gnosis.web;

import com.example.Gnosis.user.User;
import com.example.Gnosis.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class StudentAccessService {
	private final UserRepository userRepository;

	public StudentAccessService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public StudentAccessContext resolve(Authentication authentication) {
		if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
			return StudentAccessContext.inactive(null);
		}

		String studentId = authentication.getName().trim();
		return userRepository.findByStudentId(studentId)
				.map(user -> toContext(studentId, user))
				.orElse(StudentAccessContext.inactive(studentId));
	}

	public StudentAccessContext requireActiveEnrollment(Authentication authentication) {
		StudentAccessContext context = resolve(authentication);
		if (!context.canAccessClassContent()) {
			throw new IllegalArgumentException("Student is not allowed to access class content.");
		}
		return context;
	}

	public User requireStudent(Authentication authentication) {
		if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
			throw new IllegalArgumentException("Student is required.");
		}
		String studentId = authentication.getName().trim();
		return userRepository.findByStudentId(studentId)
				.orElseThrow(() -> new NoSuchElementException("Student not found."));
	}

	private static StudentAccessContext toContext(String studentId, User user) {
		String course = normalizeValue(user.getCourse());
		String section = normalizeValue(user.getSectionName());
		boolean dropped = isDropped(user.getStatus());
		return new StudentAccessContext(studentId, course, section, !dropped);
	}

	private static boolean isDropped(String status) {
		String normalized = normalizeValue(status);
		return normalized != null && normalized.toLowerCase().contains("drop");
	}

	private static String normalizeValue(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		if (trimmed.isEmpty() || "Unassigned".equalsIgnoreCase(trimmed)) {
			return null;
		}
		return trimmed;
	}

	public record StudentAccessContext(String studentId, String course, String section, boolean active) {
		static StudentAccessContext inactive(String studentId) {
			return new StudentAccessContext(studentId, null, null, false);
		}

		public boolean canAccessClassContent() {
			return active && course != null && section != null;
		}
	}
}
