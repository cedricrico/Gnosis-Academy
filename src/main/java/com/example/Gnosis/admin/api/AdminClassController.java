package com.example.Gnosis.admin.api;

import com.example.Gnosis.admin.service.AdminClassService;
import com.example.Gnosis.schoolclass.SchoolClassDto;
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
@RequestMapping("/api/admin/classes")
public class AdminClassController {
	private final AdminClassService adminClassService;

	public AdminClassController(AdminClassService adminClassService) {
		this.adminClassService = adminClassService;
	}

	@GetMapping
	public List<SchoolClassDto> listClasses() {
		return adminClassService.listClasses();
	}

	@GetMapping("/{classId}")
	public ResponseEntity<SchoolClassDto> getClassById(@PathVariable Long classId) {
		return adminClassService.getClassById(classId)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@PostMapping
	public ResponseEntity<SchoolClassDto> createClass(@RequestBody SchoolClassDto payload) {
		SchoolClassDto created = adminClassService.createClass(payload);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PutMapping("/{classId}")
	public SchoolClassDto updateClass(@PathVariable Long classId, @RequestBody SchoolClassDto payload) {
		return adminClassService.updateClass(classId, payload);
	}

	@DeleteMapping("/{classId}")
	public ResponseEntity<Void> deleteClass(@PathVariable Long classId) {
		adminClassService.deleteClass(classId);
		return ResponseEntity.noContent().build();
	}
}
