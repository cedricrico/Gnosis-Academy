package com.example.Gnosis.admin.api;

import com.example.Gnosis.admin.dto.ProfessorCreateRequest;
import com.example.Gnosis.admin.dto.ProfessorDirectoryResponse;
import com.example.Gnosis.admin.dto.ProfessorOptionResponse;
import com.example.Gnosis.admin.dto.ProfessorUpdateRequest;
import com.example.Gnosis.admin.service.AdminProfessorService;
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
@RequestMapping("/api/admin/professors")
public class AdminProfessorController {
	private final AdminProfessorService adminProfessorService;

	public AdminProfessorController(AdminProfessorService adminProfessorService) {
		this.adminProfessorService = adminProfessorService;
	}

	@GetMapping
	public List<ProfessorOptionResponse> listProfessorOptions() {
		return adminProfessorService.listOptions();
	}

	@GetMapping("/directory")
	public List<ProfessorDirectoryResponse> listProfessorDirectory() {
		return adminProfessorService.listDirectory();
	}

	@GetMapping("/{employeeId}")
	public ProfessorDirectoryResponse getProfessor(@PathVariable String employeeId) {
		return adminProfessorService.getByEmployeeId(employeeId);
	}

	@PostMapping
	public ResponseEntity<ProfessorDirectoryResponse> createProfessor(
			@Valid @RequestBody ProfessorCreateRequest payload
	) {
		ProfessorDirectoryResponse created = adminProfessorService.create(payload);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PutMapping("/{employeeId}")
	public ProfessorDirectoryResponse updateProfessor(
			@PathVariable String employeeId,
			@Valid @RequestBody ProfessorUpdateRequest payload
	) {
		return adminProfessorService.update(employeeId, payload);
	}

	@DeleteMapping("/{employeeId}")
	public ResponseEntity<Void> deleteProfessor(@PathVariable String employeeId) {
		adminProfessorService.delete(employeeId);
		return ResponseEntity.noContent().build();
	}
}
