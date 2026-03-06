package com.example.Gnosis.web;

import com.example.Gnosis.professor.Professor;
import com.example.Gnosis.professor.ProfessorRepository;
import com.example.Gnosis.schoolclass.SchoolClassDto;
import com.example.Gnosis.schoolclass.SchoolClassService;
import com.example.Gnosis.user.User;
import com.example.Gnosis.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/admin")
public class AdminClassApiController {
	private final SchoolClassService schoolClassService;
	private final ProfessorRepository professorRepository;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public AdminClassApiController(
			SchoolClassService schoolClassService,
			ProfessorRepository professorRepository,
			UserRepository userRepository,
			PasswordEncoder passwordEncoder
	) {
		this.schoolClassService = schoolClassService;
		this.professorRepository = professorRepository;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@GetMapping("/classes")
	public List<SchoolClassDto> listClasses() {
		return schoolClassService.findAll();
	}

	@GetMapping("/classes/{classId}")
	public ResponseEntity<SchoolClassDto> getClassById(@PathVariable Long classId) {
		return schoolClassService.findById(classId)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@PostMapping("/classes")
	public ResponseEntity<SchoolClassDto> createClass(@RequestBody SchoolClassDto payload) {
		SchoolClassDto created = schoolClassService.create(payload);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PutMapping("/classes/{classId}")
	public SchoolClassDto updateClass(@PathVariable Long classId, @RequestBody SchoolClassDto payload) {
		return schoolClassService.update(classId, payload);
	}

	@DeleteMapping("/classes/{classId}")
	public ResponseEntity<Void> deleteClass(@PathVariable Long classId) {
		schoolClassService.delete(classId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/professors")
	public List<ProfessorOptionResponse> listProfessorOptions() {
		return professorRepository.findAll().stream()
				.sorted(Comparator.comparing(Professor::getLastName, String.CASE_INSENSITIVE_ORDER)
						.thenComparing(Professor::getFirstName, String.CASE_INSENSITIVE_ORDER))
				.map(professor -> new ProfessorOptionResponse(
						professor.getProfessorId(),
						buildFullName(professor)
				))
				.toList();
	}

	@GetMapping("/professors/directory")
	public List<ProfessorDirectoryResponse> listProfessorDirectory() {
		return professorRepository.findAll().stream()
				.sorted(Comparator.comparing(Professor::getLastName, String.CASE_INSENSITIVE_ORDER)
						.thenComparing(Professor::getFirstName, String.CASE_INSENSITIVE_ORDER))
				.map(professor -> new ProfessorDirectoryResponse(
						professor.getProfessorId(),
						buildFullName(professor),
						professor.getDepartment(),
						professor.getPosition()
				))
				.toList();
	}

	@PostMapping("/professors")
	public ResponseEntity<ProfessorDirectoryResponse> createProfessor(@RequestBody ProfessorCreateRequest payload) {
		String firstName = requireNonBlank(payload.firstName(), "First name is required.");
		String lastName = requireNonBlank(payload.lastName(), "Last name is required.");
		Integer age = payload.age();
		if (age == null || age < 18 || age > 99) {
			throw new IllegalArgumentException("Age must be between 18 and 99.");
		}

		String sex = requireNonBlank(payload.sex(), "Sex is required.");
		String department = requireNonBlank(payload.department(), "Department is required.");
		String password = requireNonBlank(payload.password(), "Password is required.");

		Professor professor = new Professor(
				generateProfessorId(),
				firstName,
				trimToNull(payload.middleInitial()),
				lastName,
				age,
				sex,
				department,
				"Instructor",
				passwordEncoder.encode(password)
		);
		Professor saved = professorRepository.save(professor);
		return ResponseEntity.status(HttpStatus.CREATED).body(
				new ProfessorDirectoryResponse(
						saved.getProfessorId(),
						buildFullName(saved),
						saved.getDepartment(),
						saved.getPosition()
				)
		);
	}

	@GetMapping("/students")
	public List<StudentDirectoryResponse> listStudents() {
		return userRepository.findAll().stream()
				.sorted(Comparator.comparing(User::getLastName, String.CASE_INSENSITIVE_ORDER)
						.thenComparing(User::getFirstName, String.CASE_INSENSITIVE_ORDER))
				.map(user -> new StudentDirectoryResponse(
						user.getStudentId(),
						buildStudentFullName(user),
						user.getSex(),
						user.getCourse(),
						user.getSectionName(),
						normalizeStudentStatus(user.getStatus())
				))
				.toList();
	}

	@PostMapping("/students")
	public ResponseEntity<StudentDirectoryResponse> createStudent(@RequestBody StudentCreateRequest payload) {
		String studentId = requireNonBlank(payload.studentId(), "Student ID is required.");
		if (userRepository.existsByStudentId(studentId)) {
			throw new IllegalArgumentException("Student ID already exists.");
		}

		NameParts nameParts = parseFullName(payload.fullName());
		String password = requireNonBlank(payload.password(), "Password is required.");
		User user = new User(
				studentId,
				nameParts.firstName(),
				nameParts.middleInitial(),
				nameParts.lastName(),
				18,
				"Not specified",
				passwordEncoder.encode(password)
		);
		user.setCourse(trimToNull(payload.course()));
		user.setSectionName(trimToNull(payload.section()));
		user.setStatus(normalizeStudentStatus(payload.status()));

		User saved = userRepository.save(user);
		return ResponseEntity.status(HttpStatus.CREATED).body(toStudentDirectoryResponse(saved));
	}

	@PutMapping("/students/{studentId}")
	public StudentDirectoryResponse updateStudent(
			@PathVariable String studentId,
			@RequestBody StudentUpdateRequest payload
	) {
		User user = userRepository.findByStudentId(studentId)
				.orElseThrow(() -> new NoSuchElementException("Student not found: " + studentId));

		NameParts nameParts = parseFullName(payload.fullName());
		user.setFirstName(nameParts.firstName());
		user.setMiddleInitial(nameParts.middleInitial());
		user.setLastName(nameParts.lastName());
		user.setCourse(trimToNull(payload.course()));
		user.setSectionName(trimToNull(payload.section()));
		user.setStatus(normalizeStudentStatus(payload.status()));

		return toStudentDirectoryResponse(userRepository.save(user));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
	}

	private static String buildFullName(Professor professor) {
		String middleInitial = professor.getMiddleInitial();
		if (middleInitial != null && !middleInitial.isBlank()) {
			return professor.getFirstName() + " " + middleInitial + ". " + professor.getLastName();
		}
		return professor.getFirstName() + " " + professor.getLastName();
	}

	public record ProfessorOptionResponse(String professorId, String fullName) {
	}

	public record ProfessorDirectoryResponse(
			String professorId,
			String fullName,
			String department,
			String position
	) {
	}

	public record ProfessorCreateRequest(
			String firstName,
			String middleInitial,
			String lastName,
			Integer age,
			String sex,
			String department,
			String password
	) {
	}

	public record StudentDirectoryResponse(
			String studentId,
			String fullName,
			String sex,
			String course,
			String section,
			String status
	) {
	}

	public record StudentCreateRequest(
			String studentId,
			String fullName,
			String course,
			String section,
			String status,
			String password
	) {
	}

	public record StudentUpdateRequest(
			String fullName,
			String course,
			String section,
			String status
	 ) {
	}

	private static String buildStudentFullName(User user) {
		String middleInitial = user.getMiddleInitial();
		if (middleInitial != null && !middleInitial.isBlank()) {
			return user.getFirstName() + " " + middleInitial + ". " + user.getLastName();
		}
		return user.getFirstName() + " " + user.getLastName();
	}

	private static NameParts parseFullName(String fullNameRaw) {
		String fullName = requireNonBlank(fullNameRaw, "Full name is required.");
		String[] parts = fullName.trim().split("\\s+");
		if (parts.length == 1) {
			return new NameParts(parts[0], null, parts[0]);
		}

		String firstName = parts[0];
		String lastName = parts[parts.length - 1];
		String middleInitial = parts.length > 2 ? parts[1].substring(0, 1) : null;
		return new NameParts(firstName, middleInitial, lastName);
	}

	private static StudentDirectoryResponse toStudentDirectoryResponse(User user) {
		return new StudentDirectoryResponse(
				user.getStudentId(),
				buildStudentFullName(user),
				user.getSex(),
				user.getCourse(),
				user.getSectionName(),
				normalizeStudentStatus(user.getStatus())
		);
	}

	private static String normalizeStudentStatus(String raw) {
		String status = trimToNull(raw);
		return status != null ? status : "Active";
	}

	private String generateProfessorId() {
		long suffix = System.currentTimeMillis() % 1_000_000L;
		String candidate = "P" + String.format("%06d", suffix);
		while (professorRepository.existsByProfessorId(candidate)) {
			suffix = (suffix + 1) % 1_000_000L;
			candidate = "P" + String.format("%06d", suffix);
		}
		return candidate;
	}

	private static String requireNonBlank(String value, String message) {
		String cleaned = trimToNull(value);
		if (cleaned == null) {
			throw new IllegalArgumentException(message);
		}
		return cleaned;
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private record NameParts(String firstName, String middleInitial, String lastName) {
	}
}
