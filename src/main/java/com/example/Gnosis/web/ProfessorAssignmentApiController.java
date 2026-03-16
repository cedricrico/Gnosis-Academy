package com.example.Gnosis.web;

import com.example.Gnosis.assignment.AssignmentRequest;
import com.example.Gnosis.assignment.AssignmentResponse;
import com.example.Gnosis.assignment.AssignmentService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/professor/api/assignments")
public class ProfessorAssignmentApiController {
	private final AssignmentService assignmentService;

	public ProfessorAssignmentApiController(AssignmentService assignmentService) {
		this.assignmentService = assignmentService;
	}

	@GetMapping
	public List<AssignmentResponse> list(Authentication authentication) {
		ProfessorIdentity professor = resolveProfessor(authentication);
		return assignmentService.listForProfessor(professor.id())
				.stream()
				.peek(response -> {
					if (response.getAttachmentName() != null && !response.getAttachmentName().isBlank()) {
						response.setAttachmentUrl("/professor/api/assignments/" + response.getId() + "/attachment");
					}
				})
				.toList();
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<AssignmentResponse> create(
			Authentication authentication,
			@RequestParam String title,
			@RequestParam String subject,
			@RequestParam(name = "sections", required = false) List<String> sections,
			@RequestParam String description,
			@RequestParam String dueDate,
			@RequestParam String status,
			@RequestPart(name = "attachment", required = false) MultipartFile attachment
	) {
		AssignmentRequest request = new AssignmentRequest();
		request.setTitle(title);
		request.setSubject(subject);
		request.setSections(sections);
		request.setDescription(description);
		request.setDueDate(dueDate);
		request.setStatus(status);
		ProfessorIdentity professor = resolveProfessor(authentication);
		AssignmentResponse created = assignmentService.create(professor.id(), professor.name(), request, attachment);
		if (created.getAttachmentName() != null && !created.getAttachmentName().isBlank()) {
			created.setAttachmentUrl("/professor/api/assignments/" + created.getId() + "/attachment");
		}
		return ResponseEntity.ok(created);
	}

	@PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<AssignmentResponse> update(
			@PathVariable Long id,
			Authentication authentication,
			@RequestParam String title,
			@RequestParam String subject,
			@RequestParam(name = "sections", required = false) List<String> sections,
			@RequestParam String description,
			@RequestParam String dueDate,
			@RequestParam String status,
			@RequestPart(name = "attachment", required = false) MultipartFile attachment
	) {
		AssignmentRequest request = new AssignmentRequest();
		request.setTitle(title);
		request.setSubject(subject);
		request.setSections(sections);
		request.setDescription(description);
		request.setDueDate(dueDate);
		request.setStatus(status);
		ProfessorIdentity professor = resolveProfessor(authentication);
		AssignmentResponse updated = assignmentService.update(id, professor.id(), professor.name(), request, attachment);
		if (updated.getAttachmentName() != null && !updated.getAttachmentName().isBlank()) {
			updated.setAttachmentUrl("/professor/api/assignments/" + updated.getId() + "/attachment");
		}
		return ResponseEntity.ok(updated);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(
			@PathVariable Long id,
			Authentication authentication
	) {
		ProfessorIdentity professor = resolveProfessor(authentication);
		assignmentService.delete(id, professor.id());
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}/attachment")
	public ResponseEntity<Resource> downloadAttachment(
			@PathVariable Long id,
			Authentication authentication
	) {
		ProfessorIdentity professor = resolveProfessor(authentication);
		com.example.Gnosis.assignment.Assignment assignment = assignmentService.getForProfessor(id, professor.id());
		if (assignment.getAttachmentPath() == null || assignment.getAttachmentPath().isBlank()) {
			return ResponseEntity.notFound().build();
		}
		Resource resource = new FileSystemResource(assignment.getAttachmentPath());
		if (!resource.exists()) {
			return ResponseEntity.notFound().build();
		}
		String contentType = assignment.getAttachmentContentType() != null
				? assignment.getAttachmentContentType()
				: MediaType.APPLICATION_OCTET_STREAM_VALUE;
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(contentType))
				.header(HttpHeaders.CONTENT_DISPOSITION,
						"attachment; filename=\"" + assignment.getAttachmentName() + "\"")
				.body(resource);
	}

	private static ProfessorIdentity resolveProfessor(Authentication authentication) {
		if (authentication != null && authentication.getPrincipal() instanceof com.example.Gnosis.security.ProfessorUserDetails pud) {
			return new ProfessorIdentity(pud.getEmployeeId(), pud.getFullName());
		}
		String fallback = authentication != null ? authentication.getName() : "unknown";
		return new ProfessorIdentity(fallback, fallback);
	}

	private record ProfessorIdentity(String id, String name) {}
}
