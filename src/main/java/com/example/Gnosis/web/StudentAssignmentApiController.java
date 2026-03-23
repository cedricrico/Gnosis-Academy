package com.example.Gnosis.web;

import com.example.Gnosis.assignment.AssignmentResponse;
import com.example.Gnosis.assignment.AssignmentService;
import com.example.Gnosis.assignment.AssignmentSubmissionResponse;
import com.example.Gnosis.assignment.AssignmentSubmissionService;
import com.example.Gnosis.schoolclass.SchoolClassDto;
import com.example.Gnosis.schoolclass.SchoolClassService;
import com.example.Gnosis.user.User;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

@RestController
@RequestMapping("/student/api/assignments")
public class StudentAssignmentApiController {
	private final AssignmentService assignmentService;
	private final SchoolClassService schoolClassService;
	private final AssignmentSubmissionService submissionService;
	private final StudentAccessService studentAccessService;

	public StudentAssignmentApiController(
			AssignmentService assignmentService,
			SchoolClassService schoolClassService,
			AssignmentSubmissionService submissionService,
			StudentAccessService studentAccessService
	) {
		this.assignmentService = assignmentService;
		this.schoolClassService = schoolClassService;
		this.submissionService = submissionService;
		this.studentAccessService = studentAccessService;
	}

	@GetMapping
	public List<AssignmentResponse> list(Authentication authentication) {
		StudentAccessService.StudentAccessContext context = studentAccessService.resolve(authentication);
		if (!context.canAccessClassContent()) {
			return List.of();
		}
		User student = studentAccessService.requireStudent(authentication);
		Set<String> allowedSubjects = resolveAllowedSubjects(context);
		java.util.Map<Long, AssignmentSubmissionResponse> submissionsByAssignmentId =
				submissionService.listForStudent(student.getStudentId()).stream()
						.collect(java.util.stream.Collectors.toMap(
								AssignmentSubmissionResponse::getAssignmentId,
								response -> response,
								(existing, replacement) -> existing
						));
		return assignmentService.listForStudentSection(context.section(), allowedSubjects)
				.stream()
				.peek(response -> {
					if (response.getAttachmentName() != null && !response.getAttachmentName().isBlank()) {
						response.setAttachmentUrl("/student/api/assignments/" + response.getId() + "/attachment");
					}
					AssignmentSubmissionResponse submission = submissionsByAssignmentId.get(response.getId());
					if (submission != null) {
						response.setSubmitted(true);
						response.setSubmissionId(submission.getId());
						response.setSubmittedAt(submission.getSubmittedAt());
						response.setSubmissionGrade(submission.getGrade());
						response.setGradedAt(submission.getGradedAt());
					}
				})
				.toList();
	}

	@GetMapping("/{id}/attachment")
	public ResponseEntity<Resource> downloadAttachment(
			@PathVariable Long id,
			Authentication authentication
	) {
		StudentAccessService.StudentAccessContext context = studentAccessService.requireActiveEnrollment(authentication);
		Set<String> allowedSubjects = resolveAllowedSubjects(context);
		com.example.Gnosis.assignment.Assignment assignment = assignmentService.getForStudent(id, context.section(), allowedSubjects);
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

	@GetMapping("/{id}/preview")
	public ResponseEntity<Resource> previewAttachment(
			@PathVariable Long id,
			Authentication authentication
	) {
		StudentAccessService.StudentAccessContext context = studentAccessService.requireActiveEnrollment(authentication);
		Set<String> allowedSubjects = resolveAllowedSubjects(context);
		com.example.Gnosis.assignment.Assignment assignment = assignmentService.getForStudent(id, context.section(), allowedSubjects);
		if (assignment.getAttachmentPath() == null || assignment.getAttachmentPath().isBlank()) {
			return ResponseEntity.notFound().build();
		}
		Resource resource = new FileSystemResource(assignment.getAttachmentPath());
		if (!resource.exists()) {
			return ResponseEntity.notFound().build();
		}
		String contentType = assignment.getAttachmentContentType();
		if (!isPreviewable(contentType, assignment.getAttachmentName())) {
			return ResponseEntity.status(415).build();
		}
		String resolvedContentType = contentType != null
				? contentType
				: MediaType.APPLICATION_OCTET_STREAM_VALUE;
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(resolvedContentType))
				.header(HttpHeaders.CONTENT_DISPOSITION,
						"inline; filename=\"" + assignment.getAttachmentName() + "\"")
				.body(resource);
	}

	@PostMapping(value = "/{id}/submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<AssignmentSubmissionResponse> submitAssignment(
			@PathVariable Long id,
			Authentication authentication,
			@RequestPart("file") MultipartFile file
	) {
		StudentAccessService.StudentAccessContext context = studentAccessService.requireActiveEnrollment(authentication);
		Set<String> allowedSubjects = resolveAllowedSubjects(context);
		assignmentService.getForStudent(id, context.section(), allowedSubjects);
		User student = studentAccessService.requireStudent(authentication);
		AssignmentSubmissionResponse response = submissionService.submit(id, student, file);
		response.setDownloadUrl("/student/api/assignments/submissions/" + response.getId() + "/download");
		return ResponseEntity.ok(response);
	}

	@GetMapping("/submissions/{id}/download")
	public ResponseEntity<Resource> downloadSubmission(
			@PathVariable Long id,
			Authentication authentication
	) {
		studentAccessService.requireActiveEnrollment(authentication);
		User student = studentAccessService.requireStudent(authentication);
		if (student == null) {
			return ResponseEntity.status(401).build();
		}
		com.example.Gnosis.assignment.AssignmentSubmission submission =
				submissionService.getForStudent(id, student.getStudentId());
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

	private Set<String> resolveAllowedSubjects(StudentAccessService.StudentAccessContext context) {
		if (context.course() == null || context.section() == null) {
			return Set.of();
		}
		List<SchoolClassDto> classes = schoolClassService.findForStudent(context.course(), context.section());
		LinkedHashSet<String> subjects = new LinkedHashSet<>();
		for (SchoolClassDto schoolClass : classes) {
			for (SchoolClassDto.SubjectDto subject : schoolClass.getSubjects()) {
				String code = normalizeValue(subject.getCode());
				String name = normalizeValue(subject.getName());
				if (code != null && name != null) {
					subjects.add(code + " - " + name);
				}
				if (code != null) {
					subjects.add(code);
				}
				if (name != null) {
					subjects.add(name);
				}
			}
		}
		return subjects;
	}

	private static String normalizeValue(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		if ("Unassigned".equalsIgnoreCase(trimmed)) {
			return null;
		}
		return trimmed;
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
}
