package com.example.Gnosis.assignment;

import com.example.Gnosis.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

@Service
public class AssignmentSubmissionService {
	private final AssignmentSubmissionRepository submissionRepository;
	private final AssignmentRepository assignmentRepository;
	private final Path attachmentRoot = Paths.get("uploads", "assignment-submissions");
	private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx", "jpg", "jpeg", "png");
	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
			"application/pdf",
			"application/msword",
			"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
			"image/jpeg",
			"image/png"
	);

	public AssignmentSubmissionService(
			AssignmentSubmissionRepository submissionRepository,
			AssignmentRepository assignmentRepository
	) {
		this.submissionRepository = submissionRepository;
		this.assignmentRepository = assignmentRepository;
	}

	@Transactional(readOnly = true)
	public List<AssignmentSubmissionResponse> listForProfessor(String professorId) {
		return submissionRepository.findByProfessorIdOrderBySubmittedAtDesc(professorId)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<AssignmentSubmissionResponse> listForStudent(String studentId) {
		return submissionRepository.findByStudentIdOrderBySubmittedAtDesc(studentId)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	public AssignmentSubmissionResponse submit(Long assignmentId, User student, MultipartFile file) {
		if (assignmentId == null) {
			throw new IllegalArgumentException("Assignment is required.");
		}
		if (student == null) {
			throw new IllegalArgumentException("Student is required.");
		}
		Assignment assignment = assignmentRepository.findById(assignmentId)
				.orElseThrow(() -> new NoSuchElementException("Assignment not found."));
		ensureAssignmentOpen(assignment);
		if (submissionRepository.existsByAssignmentIdAndStudentId(assignmentId, student.getStudentId())) {
			throw new IllegalArgumentException("You have already submitted this assignment.");
		}
		validateAttachment(file);

		AssignmentSubmission submission = new AssignmentSubmission();
		submission.setAssignmentId(assignment.getId());
		submission.setAssignmentTitle(assignment.getTitle());
		submission.setAssignmentSubject(assignment.getSubject());
		submission.setProfessorId(assignment.getProfessorId());
		submission.setStudentId(student.getStudentId());
		submission.setStudentName(formatStudentName(student));
		submission.setStudentCourse(student.getCourse());
		submission.setStudentSection(student.getSectionName());
		submission.setAssignmentPoints(assignment.getPoints());

		storeFile(submission, file);
		AssignmentSubmission saved = submissionRepository.save(submission);
		return toResponse(saved);
	}

	@Transactional
	public AssignmentSubmissionResponse gradeSubmission(Long id, String professorId, Integer grade, String feedback) {
		AssignmentSubmission submission = submissionRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Submission not found."));
		if (!submission.getProfessorId().equals(professorId)) {
			throw new IllegalArgumentException("Unauthorized submission access.");
		}
		if (grade == null) {
			throw new IllegalArgumentException("Grade is required.");
		}
		if (grade < 0) {
			throw new IllegalArgumentException("Grade must be 0 or higher.");
		}
		Integer maxPoints = submission.getAssignmentPoints();
		if (maxPoints != null && grade > maxPoints) {
			throw new IllegalArgumentException("Grade cannot exceed the assignment points.");
		}
		submission.setGrade(grade);
		submission.setFeedback(trimToNull(feedback));
		submission.setGradedAt(Instant.now());
		return toResponse(submissionRepository.save(submission));
	}

	@Transactional(readOnly = true)
	public AssignmentSubmission getForProfessor(Long id, String professorId) {
		AssignmentSubmission submission = submissionRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Submission not found."));
		if (!submission.getProfessorId().equals(professorId)) {
			throw new IllegalArgumentException("Unauthorized submission access.");
		}
		return submission;
	}

	@Transactional(readOnly = true)
	public AssignmentSubmission getForStudent(Long id, String studentId) {
		AssignmentSubmission submission = submissionRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Submission not found."));
		if (!submission.getStudentId().equals(studentId)) {
			throw new IllegalArgumentException("Unauthorized submission access.");
		}
		return submission;
	}

	private void storeFile(AssignmentSubmission submission, MultipartFile attachment) {
		try {
			Files.createDirectories(attachmentRoot);
			String originalName = attachment.getOriginalFilename();
			String extension = extractExtension(originalName);
			String filename = "submission-" + submission.getAssignmentId() + "-" + submission.getStudentId()
					+ "-" + UUID.randomUUID() + "." + extension;
			Path target = attachmentRoot.resolve(filename).normalize();
			try (InputStream inputStream = attachment.getInputStream()) {
				Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
			}
			submission.setFilename(originalName != null ? originalName : filename);
			submission.setFilePath(target.toString());
			submission.setContentType(attachment.getContentType());
			submission.setFileSize(attachment.getSize());
		} catch (IOException ex) {
			throw new IllegalStateException("Unable to store submission.", ex);
		}
	}

	private AssignmentSubmissionResponse toResponse(AssignmentSubmission submission) {
		AssignmentSubmissionResponse response = new AssignmentSubmissionResponse();
		response.setId(submission.getId());
		response.setAssignmentId(submission.getAssignmentId());
		response.setAssignmentTitle(submission.getAssignmentTitle());
		response.setAssignmentSubject(submission.getAssignmentSubject());
		response.setStudentId(submission.getStudentId());
		response.setStudentName(submission.getStudentName());
		response.setStudentCourse(submission.getStudentCourse());
		response.setStudentSection(submission.getStudentSection());
		response.setAssignmentPoints(submission.getAssignmentPoints());
		response.setFilename(submission.getFilename());
		response.setContentType(submission.getContentType());
		response.setGrade(submission.getGrade());
		response.setFeedback(submission.getFeedback());
		response.setGradedAt(submission.getGradedAt());
		response.setSubmittedAt(submission.getSubmittedAt());
		return response;
	}

	private void ensureAssignmentOpen(Assignment assignment) {
		String status = assignment.getStatus();
		if (status != null) {
			String normalized = status.trim().toLowerCase();
			if ("draft".equals(normalized) || "expired".equals(normalized)) {
				throw new IllegalArgumentException("Assignment is not open for submissions.");
			}
		}
		if (isDueDatePast(assignment.getDueDate())) {
			throw new IllegalArgumentException("Assignment is not open for submissions.");
		}
	}

	private static boolean isDueDatePast(String dueDate) {
		if (dueDate == null || dueDate.isBlank()) {
			return false;
		}
		String normalized = dueDate.contains("T") ? dueDate : dueDate + "T00:00";
		try {
			LocalDateTime due = LocalDateTime.parse(normalized);
			return due.atZone(ZoneId.systemDefault()).toInstant().isBefore(java.time.Instant.now());
		} catch (Exception ex) {
			return false;
		}
	}

	private static void validateAttachment(MultipartFile attachment) {
		if (attachment == null || attachment.isEmpty()) {
			throw new IllegalArgumentException("Please attach a file.");
		}
		String originalName = attachment.getOriginalFilename();
		String extension = extractExtension(originalName);
		String contentType = attachment.getContentType();

		boolean extensionAllowed = extension != null && ALLOWED_EXTENSIONS.contains(extension.toLowerCase());
		boolean contentTypeAllowed = contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType);

		if (!extensionAllowed && !contentTypeAllowed) {
			throw new IllegalArgumentException("Unsupported file type. Allowed: PDF, DOC, DOCX, JPG, PNG.");
		}
	}

	private static String extractExtension(String filename) {
		if (filename == null) {
			return "bin";
		}
		int index = filename.lastIndexOf('.');
		if (index < 0 || index == filename.length() - 1) {
			return "bin";
		}
		return filename.substring(index + 1).toLowerCase();
	}

	private static String formatStudentName(User user) {
		String first = user.getFirstName() != null ? user.getFirstName().trim() : "";
		String last = user.getLastName() != null ? user.getLastName().trim() : "";
		String mi = user.getMiddleInitial() != null ? user.getMiddleInitial().trim() : "";

		StringBuilder sb = new StringBuilder();
		if (!first.isEmpty()) {
			sb.append(first);
		}
		if (!mi.isEmpty()) {
			if (sb.length() > 0) sb.append(' ');
			sb.append(mi).append('.');
		}
		if (!last.isEmpty()) {
			if (sb.length() > 0) sb.append(' ');
			sb.append(last);
		}
		String full = sb.toString().trim();
		return full.isEmpty() ? user.getStudentId() : full;
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
