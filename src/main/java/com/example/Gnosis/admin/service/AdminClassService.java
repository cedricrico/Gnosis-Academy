package com.example.Gnosis.admin.service;

import com.example.Gnosis.admin.dto.AdminClassStudentAssignRequest;
import com.example.Gnosis.admin.dto.AdminMasterlistStudentDto;
import com.example.Gnosis.schoolclass.SchoolClassDto;
import com.example.Gnosis.schoolclass.SchoolClassService;
import com.example.Gnosis.user.User;
import com.example.Gnosis.user.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class AdminClassService {
	private final SchoolClassService schoolClassService;
	private final UserRepository userRepository;

	public AdminClassService(SchoolClassService schoolClassService, UserRepository userRepository) {
		this.schoolClassService = schoolClassService;
		this.userRepository = userRepository;
	}

	public List<SchoolClassDto> listClasses() {
		return schoolClassService.findAll();
	}

	public Optional<SchoolClassDto> getClassById(Long classId) {
		return schoolClassService.findById(classId);
	}

	public SchoolClassDto createClass(SchoolClassDto payload) {
		return schoolClassService.create(payload);
	}

	public SchoolClassDto updateClass(Long classId, SchoolClassDto payload) {
		return schoolClassService.update(classId, payload);
	}

	public void deleteClass(Long classId) {
		schoolClassService.delete(classId);
	}

	public List<AdminMasterlistStudentDto> listMasterlist(Long classId) {
		SchoolClassDto schoolClass = schoolClassService.findById(classId)
				.orElseThrow(() -> new NoSuchElementException("Class not found: " + classId));

		String course = trimToNull(schoolClass.getCourseName());
		String sectionName = trimToNull(schoolClass.getSectionName());
		if (course == null || sectionName == null) {
			return List.of();
		}

		return userRepository.findByCourseIgnoreCaseAndSectionNameIgnoreCaseOrderByLastNameAscFirstNameAsc(course, sectionName)
				.stream()
				.map(user -> new AdminMasterlistStudentDto(
						user.getStudentId(),
						buildFullName(user),
						normalizeStatus(user.getStatus())
				))
				.toList();
	}

	public void assignStudentToClass(Long classId, AdminClassStudentAssignRequest payload) {
		if (payload == null || trimToNull(payload.studentId()) == null) {
			throw new IllegalArgumentException("Student ID is required.");
		}

		SchoolClassDto schoolClass = schoolClassService.findById(classId)
				.orElseThrow(() -> new NoSuchElementException("Class not found: " + classId));

		String course = trimToNull(schoolClass.getCourseName());
		String sectionName = trimToNull(schoolClass.getSectionName());
		if (course == null || sectionName == null) {
			throw new IllegalArgumentException("Class is missing course/section.");
		}

		String studentId = payload.studentId().trim();
		User user = userRepository.findByStudentId(studentId)
				.orElseThrow(() -> new NoSuchElementException("Student not found: " + studentId));

		user.setCourse(course);
		user.setSectionName(sectionName);
		user.setStatus("Enrolled");
		userRepository.save(user);
	}

	public void dropStudent(Long classId, String studentId) {
		SchoolClassDto schoolClass = schoolClassService.findById(classId)
				.orElseThrow(() -> new NoSuchElementException("Class not found: " + classId));

		String course = trimToNull(schoolClass.getCourseName());
		String sectionName = trimToNull(schoolClass.getSectionName());
		if (course == null || sectionName == null) {
			throw new IllegalArgumentException("Class is missing course/section.");
		}

		String studentIdNormalized = trimToNull(studentId);
		if (studentIdNormalized == null) {
			throw new IllegalArgumentException("Student ID is required.");
		}

		User user = userRepository.findByStudentId(studentIdNormalized)
				.orElseThrow(() -> new NoSuchElementException("Student not found: " + studentIdNormalized));

		if (!equalsIgnoreCase(trimToNull(user.getCourse()), course)
				|| !equalsIgnoreCase(trimToNull(user.getSectionName()), sectionName)) {
			throw new IllegalArgumentException("Student is not enrolled in the selected class.");
		}

		user.setStatus("Dropped");
		userRepository.save(user);
	}

	public void reenrollStudent(Long classId, String studentId) {
		SchoolClassDto schoolClass = schoolClassService.findById(classId)
				.orElseThrow(() -> new NoSuchElementException("Class not found: " + classId));

		String course = trimToNull(schoolClass.getCourseName());
		String sectionName = trimToNull(schoolClass.getSectionName());
		if (course == null || sectionName == null) {
			throw new IllegalArgumentException("Class is missing course/section.");
		}

		String studentIdNormalized = trimToNull(studentId);
		if (studentIdNormalized == null) {
			throw new IllegalArgumentException("Student ID is required.");
		}

		User user = userRepository.findByStudentId(studentIdNormalized)
				.orElseThrow(() -> new NoSuchElementException("Student not found: " + studentIdNormalized));

		if (!equalsIgnoreCase(trimToNull(user.getCourse()), course)
				|| !equalsIgnoreCase(trimToNull(user.getSectionName()), sectionName)) {
			throw new IllegalArgumentException("Student is not enrolled in the selected class.");
		}

		user.setStatus("Enrolled");
		userRepository.save(user);
	}

	private static String buildFullName(User user) {
		String first = trimToNull(user.getFirstName());
		String middleInitial = trimToNull(user.getMiddleInitial());
		String last = trimToNull(user.getLastName());

		StringBuilder sb = new StringBuilder();
		if (first != null) {
			sb.append(first);
		}
		if (middleInitial != null) {
			if (sb.length() > 0) {
				sb.append(' ');
			}
			sb.append(middleInitial).append('.');
		}
		if (last != null) {
			if (sb.length() > 0) {
				sb.append(' ');
			}
			sb.append(last);
		}
		return sb.length() == 0 ? user.getStudentId() : sb.toString();
	}

	private static String normalizeStatus(String status) {
		String value = trimToNull(status);
		if (value == null) {
			return "Enrolled";
		}
		String lower = value.toLowerCase(Locale.ROOT);
		if (lower.contains("drop")) {
			return "Dropped";
		}
		if (lower.contains("enroll")) {
			return "Enrolled";
		}
		return value;
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static boolean equalsIgnoreCase(String left, String right) {
		if (left == null || right == null) {
			return false;
		}
		return left.equalsIgnoreCase(right);
	}
}
