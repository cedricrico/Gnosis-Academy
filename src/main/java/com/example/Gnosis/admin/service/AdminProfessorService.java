package com.example.Gnosis.admin.service;

import com.example.Gnosis.admin.dto.ProfessorCreateRequest;
import com.example.Gnosis.admin.dto.ProfessorDirectoryResponse;
import com.example.Gnosis.admin.dto.ProfessorOptionResponse;
import com.example.Gnosis.admin.dto.ProfessorUpdateRequest;
import com.example.Gnosis.professor.Professor;
import com.example.Gnosis.professor.ProfessorRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

@Service
public class AdminProfessorService {
	private static final Pattern EMPLOYEE_ID_PATTERN = Pattern.compile("^\\d{4}-\\d{5}$");

	private final ProfessorRepository professorRepository;
	private final PasswordEncoder passwordEncoder;

	public AdminProfessorService(
			ProfessorRepository professorRepository,
			PasswordEncoder passwordEncoder
	) {
		this.professorRepository = professorRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public List<ProfessorOptionResponse> listOptions() {
		return professorRepository.findAll().stream()
				.sorted(Comparator.comparing(Professor::getLastName, String.CASE_INSENSITIVE_ORDER)
						.thenComparing(Professor::getFirstName, String.CASE_INSENSITIVE_ORDER))
				.map(professor -> new ProfessorOptionResponse(
						professor.getEmployeeId(),
						buildFullName(professor)
				))
				.toList();
	}

	public List<ProfessorDirectoryResponse> listDirectory() {
		return professorRepository.findAll().stream()
				.sorted(Comparator.comparing(Professor::getLastName, String.CASE_INSENSITIVE_ORDER)
						.thenComparing(Professor::getFirstName, String.CASE_INSENSITIVE_ORDER))
				.map(this::toDirectoryResponse)
				.toList();
	}

	public ProfessorDirectoryResponse getByEmployeeId(String employeeId) {
		return professorRepository.findByEmployeeId(employeeId)
				.map(this::toDirectoryResponse)
				.orElseThrow(() -> new NoSuchElementException("Professor not found: " + employeeId));
	}

	public ProfessorDirectoryResponse create(ProfessorCreateRequest payload) {
		String employeeId = payload.employeeId().trim();
		if (!EMPLOYEE_ID_PATTERN.matcher(employeeId).matches()) {
			throw new IllegalArgumentException("Employee ID must match 0000-00000.");
		}
		if (professorRepository.existsByEmployeeId(employeeId)) {
			throw new IllegalArgumentException("Employee ID already exists.");
		}

		String email = payload.email().trim();
		if (professorRepository.existsByEmail(email)) {
			throw new IllegalArgumentException("Email already exists.");
		}

		Professor professor = new Professor(
				employeeId,
				payload.firstName().trim(),
				trimToNull(payload.middleInitial()),
				payload.lastName().trim(),
				payload.department().trim(),
				email,
				passwordEncoder.encode(payload.password())
		);
		return toDirectoryResponse(professorRepository.save(professor));
	}

	public ProfessorDirectoryResponse update(String employeeId, ProfessorUpdateRequest payload) {
		Professor professor = professorRepository.findByEmployeeId(employeeId)
				.orElseThrow(() -> new NoSuchElementException("Professor not found: " + employeeId));

		String updatedEmployeeId = payload.employeeId().trim();
		if (!updatedEmployeeId.equalsIgnoreCase(professor.getEmployeeId())
				&& !EMPLOYEE_ID_PATTERN.matcher(updatedEmployeeId).matches()) {
			throw new IllegalArgumentException("Employee ID must match 0000-00000.");
		}
		if (!updatedEmployeeId.equalsIgnoreCase(professor.getEmployeeId())
				&& professorRepository.existsByEmployeeId(updatedEmployeeId)) {
			throw new IllegalArgumentException("Employee ID already exists.");
		}

		String email = payload.email().trim();
		if (!email.equalsIgnoreCase(professor.getEmail())
				&& professorRepository.findByEmail(email).isPresent()) {
			throw new IllegalArgumentException("Email already exists.");
		}

		professor.setEmployeeId(updatedEmployeeId);
		professor.setFirstName(payload.firstName().trim());
		professor.setMiddleInitial(trimToNull(payload.middleInitial()));
		professor.setLastName(payload.lastName().trim());
		professor.setDepartment(payload.department().trim());
		professor.setEmail(email);

		return toDirectoryResponse(professorRepository.save(professor));
	}

	public void delete(String employeeId) {
		Professor professor = professorRepository.findByEmployeeId(employeeId)
				.orElseThrow(() -> new NoSuchElementException("Professor not found: " + employeeId));
		professorRepository.delete(professor);
	}

	public void deleteById(Long id) {
		Professor professor = professorRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Professor not found: " + id));
		professorRepository.delete(professor);
	}

	private ProfessorDirectoryResponse toDirectoryResponse(Professor professor) {
		return new ProfessorDirectoryResponse(
				professor.getId(),
				professor.getEmployeeId(),
				buildFullName(professor),
				professor.getDepartment(),
				professor.getEmail()
		);
	}

	private static String buildFullName(Professor professor) {
		String middleInitial = professor.getMiddleInitial();
		if (middleInitial != null && !middleInitial.isBlank()) {
			return professor.getFirstName() + " " + middleInitial + ". " + professor.getLastName();
		}
		return professor.getFirstName() + " " + professor.getLastName();
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
