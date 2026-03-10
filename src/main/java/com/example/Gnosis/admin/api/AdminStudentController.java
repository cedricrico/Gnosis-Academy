package com.example.Gnosis.admin.api;

import com.example.Gnosis.admin.dto.StudentCreateRequest;
import com.example.Gnosis.admin.dto.StudentDirectoryResponse;
import com.example.Gnosis.admin.dto.StudentUpdateRequest;
import com.example.Gnosis.admin.service.AdminStudentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/students")
public class AdminStudentController {
	private final AdminStudentService adminStudentService;

	public AdminStudentController(AdminStudentService adminStudentService) {
		this.adminStudentService = adminStudentService;
	}

	@GetMapping
	public List<StudentDirectoryResponse> listStudents() {
		return adminStudentService.listStudents();
	}

	@GetMapping("/{studentId}")
	public StudentDirectoryResponse getStudent(@PathVariable String studentId) {
		return adminStudentService.getStudent(studentId);
	}

	@PostMapping
	public ResponseEntity<StudentDirectoryResponse> createStudent(
			@Valid @RequestBody StudentCreateRequest payload
	) {
		StudentDirectoryResponse created = adminStudentService.createStudent(payload);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PutMapping("/{studentId}")
	public StudentDirectoryResponse updateStudent(
			@PathVariable String studentId,
			@Valid @RequestBody StudentUpdateRequest payload
	) {
		return adminStudentService.updateStudent(studentId, payload);
	}

	@DeleteMapping("/{studentId}")
	public ResponseEntity<Void> deleteStudent(@PathVariable String studentId) {
		adminStudentService.deleteStudent(studentId);
		return ResponseEntity.noContent().build();
	}
}
