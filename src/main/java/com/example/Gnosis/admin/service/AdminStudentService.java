package com.example.Gnosis.admin.service;

import com.example.Gnosis.admin.dto.StudentCreateRequest;
import com.example.Gnosis.admin.dto.StudentDirectoryResponse;
import com.example.Gnosis.admin.dto.StudentUpdateRequest;
import com.example.Gnosis.user.User;
import com.example.Gnosis.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

@Service
public class AdminStudentService {
	private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("^\\d{4}-\\d{5}$");

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public AdminStudentService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public List<StudentDirectoryResponse> listStudents() {
		return userRepository.findAll().stream()
				.sorted(Comparator.comparing(User::getLastName, String.CASE_INSENSITIVE_ORDER)
						.thenComparing(User::getFirstName, String.CASE_INSENSITIVE_ORDER))
				.map(AdminStudentService::toDirectoryResponse)
				.toList();
	}

	public StudentDirectoryResponse getStudent(String studentId) {
		return userRepository.findByStudentId(studentId)
				.map(AdminStudentService::toDirectoryResponse)
				.orElseThrow(() -> new NoSuchElementException("Student not found: " + studentId));
	}

	public StudentDirectoryResponse createStudent(StudentCreateRequest payload) {
		String studentId = payload.studentId().trim();
		if (!STUDENT_ID_PATTERN.matcher(studentId).matches()) {
			throw new IllegalArgumentException("Student ID must match 0000-00000.");
		}
		if (userRepository.existsByStudentId(studentId)) {
			throw new IllegalArgumentException("Student ID already exists.");
		}

		NameParts nameParts = parseFullName(payload.fullName());

		User user = new User(
				studentId,
				nameParts.firstName(),
				nameParts.middleInitial(),
				nameParts.lastName(),
				18,
				"Not specified",
				passwordEncoder.encode(payload.password())
		);
		user.setCourse(trimToNull(payload.course()));
		user.setSectionName(trimToNull(payload.section()));
		user.setStatus(normalizeStatus(payload.status()));

		return toDirectoryResponse(userRepository.save(user));
	}

	public StudentDirectoryResponse updateStudent(String studentId, StudentUpdateRequest payload) {
		User user = userRepository.findByStudentId(studentId)
				.orElseThrow(() -> new NoSuchElementException("Student not found: " + studentId));

		String updatedStudentId = payload.studentId().trim();
		if (!STUDENT_ID_PATTERN.matcher(updatedStudentId).matches()) {
			throw new IllegalArgumentException("Student ID must match 0000-00000.");
		}
		if (!updatedStudentId.equalsIgnoreCase(user.getStudentId())
				&& userRepository.existsByStudentId(updatedStudentId)) {
			throw new IllegalArgumentException("Student ID already exists.");
		}

		NameParts nameParts = parseFullName(payload.fullName());
		user.setStudentId(updatedStudentId);
		user.setFirstName(nameParts.firstName());
		user.setMiddleInitial(nameParts.middleInitial());
		user.setLastName(nameParts.lastName());
		if (payload.course() != null) {
			user.setCourse(trimToNull(payload.course()));
		}
		if (payload.section() != null) {
			user.setSectionName(trimToNull(payload.section()));
		}
		if (payload.status() != null) {
			user.setStatus(normalizeStatus(payload.status()));
		}

		return toDirectoryResponse(userRepository.save(user));
	}

	public void deleteStudent(String studentId) {
		User user = userRepository.findByStudentId(studentId)
				.orElseThrow(() -> new NoSuchElementException("Student not found: " + studentId));
		userRepository.delete(user);
	}

	private static StudentDirectoryResponse toDirectoryResponse(User user) {
		return new StudentDirectoryResponse(
				user.getStudentId(),
				buildFullName(user),
				user.getSex(),
				user.getCourse(),
				user.getSectionName(),
				normalizeStatus(user.getStatus())
		);
	}

	private static String buildFullName(User user) {
		String middleInitial = user.getMiddleInitial();
		if (middleInitial != null && !middleInitial.isBlank()) {
			return user.getFirstName() + " " + middleInitial + ". " + user.getLastName();
		}
		return user.getFirstName() + " " + user.getLastName();
	}

	private static String normalizeStatus(String raw) {
		String status = trimToNull(raw);
		return status != null ? status : "Active";
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static NameParts parseFullName(String fullNameRaw) {
		String fullName = trimToNull(fullNameRaw);
		if (fullName == null) {
			throw new IllegalArgumentException("Full name is required.");
		}
		String[] parts = fullName.trim().split("\\s+");
		if (parts.length == 1) {
			return new NameParts(parts[0], null, parts[0]);
		}

		String firstName = parts[0];
		String lastName = parts[parts.length - 1];
		String middleInitial = parts.length > 2 ? parts[1].substring(0, 1) : null;
		return new NameParts(firstName, middleInitial, lastName);
	}

	private record NameParts(String firstName, String middleInitial, String lastName) {
	}
}
