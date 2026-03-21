package com.example.Gnosis.web;

import com.example.Gnosis.assignment.AssignmentSubmissionResponse;
import com.example.Gnosis.assignment.AssignmentSubmissionService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/professor/api/assignment-submissions")
public class ProfessorAssignmentSubmissionApiController {
	private final AssignmentSubmissionService submissionService;

	public ProfessorAssignmentSubmissionApiController(AssignmentSubmissionService submissionService) {
		this.submissionService = submissionService;
	}

	@GetMapping
	public List<AssignmentSubmissionResponse> list(Authentication authentication) {
		ProfessorIdentity professor = resolveProfessor(authentication);
		return submissionService.listForProfessor(professor.id())
				.stream()
				.peek(response -> response.setDownloadUrl("/professor/api/assignment-submissions/" + response.getId() + "/download"))
				.toList();
	}

	@PutMapping("/{id}/grade")
	public AssignmentSubmissionResponse grade(
			@PathVariable Long id,
			@RequestBody GradeRequest request,
			Authentication authentication
	) {
		ProfessorIdentity professor = resolveProfessor(authentication);
		AssignmentSubmissionResponse response = submissionService.gradeSubmission(
				id,
				professor.id(),
				request.grade(),
				request.feedback()
		);
		response.setDownloadUrl("/professor/api/assignment-submissions/" + response.getId() + "/download");
		return response;
	}

	@GetMapping("/{id}/download")
	public ResponseEntity<Resource> download(
			@PathVariable Long id,
			Authentication authentication
	) {
		ProfessorIdentity professor = resolveProfessor(authentication);
		com.example.Gnosis.assignment.AssignmentSubmission submission =
				submissionService.getForProfessor(id, professor.id());
		Resource resource = new FileSystemResource(submission.getFilePath());
		if (!resource.exists()) {
			return ResponseEntity.notFound().build();
		}
		String contentType = submission.getContentType() != null
				? submission.getContentType()
				: MediaType.APPLICATION_OCTET_STREAM_VALUE;
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(contentType))
				.header(HttpHeaders.CONTENT_DISPOSITION,
						"attachment; filename=\"" + submission.getFilename() + "\"")
				.body(resource);
	}

	@GetMapping("/{id}/preview")
	public ResponseEntity<Resource> preview(
			@PathVariable Long id,
			Authentication authentication
	) {
		ProfessorIdentity professor = resolveProfessor(authentication);
		com.example.Gnosis.assignment.AssignmentSubmission submission =
				submissionService.getForProfessor(id, professor.id());
		Resource resource = new FileSystemResource(submission.getFilePath());
		if (!resource.exists()) {
			return ResponseEntity.notFound().build();
		}
		String contentType = submission.getContentType();
		if (!isPreviewable(contentType, submission.getFilename())) {
			return ResponseEntity.status(415).build();
		}
		String resolvedContentType = contentType != null
				? contentType
				: MediaType.APPLICATION_OCTET_STREAM_VALUE;
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(resolvedContentType))
				.header(HttpHeaders.CONTENT_DISPOSITION,
						"inline; filename=\"" + submission.getFilename() + "\"")
				.body(resource);
	}

	private static ProfessorIdentity resolveProfessor(Authentication authentication) {
		if (authentication != null && authentication.getPrincipal() instanceof com.example.Gnosis.security.ProfessorUserDetails pud) {
			return new ProfessorIdentity(pud.getEmployeeId(), pud.getFullName());
		}
		String fallback = authentication != null ? authentication.getName() : "unknown";
		return new ProfessorIdentity(fallback, fallback);
	}

	private static boolean isPreviewable(String contentType, String filename) {
		if (contentType != null) {
			String lowered = contentType.toLowerCase();
			return lowered.equals("application/pdf") || lowered.startsWith("image/");
		}
		if (filename == null) {
			return false;
		}
		String lowered = filename.toLowerCase();
		return lowered.endsWith(".pdf") || lowered.endsWith(".png") || lowered.endsWith(".jpg") || lowered.endsWith(".jpeg");
	}

	private record ProfessorIdentity(String id, String name) {}
	private record GradeRequest(Integer grade, String feedback) {}
}
