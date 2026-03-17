package com.example.Gnosis.assignment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.Gnosis.activity.ProfessorActivityService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

@Service
public class AssignmentService {
	private final AssignmentRepository assignmentRepository;
	private final ProfessorActivityService activityService;
	private final Path attachmentRoot = Paths.get("uploads", "assignments");
	private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx", "ppt", "pptx");
	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
			"application/pdf",
			"application/msword",
			"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
			"application/vnd.ms-powerpoint",
			"application/vnd.openxmlformats-officedocument.presentationml.presentation"
	);

	public AssignmentService(AssignmentRepository assignmentRepository, ProfessorActivityService activityService) {
		this.assignmentRepository = assignmentRepository;
		this.activityService = activityService;
	}

	@Transactional(readOnly = true)
	public List<AssignmentResponse> listForProfessor(String professorId) {
		return assignmentRepository.findByProfessorIdOrderByCreatedAtDesc(professorId)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	public AssignmentResponse create(
			String professorId,
			String professorName,
			AssignmentRequest request,
			MultipartFile attachment
	) {
		Assignment assignment = new Assignment();
		applyRequest(assignment, professorId, professorName, request);
		Assignment saved = assignmentRepository.save(assignment);
		if (attachment != null && !attachment.isEmpty()) {
			storeAttachment(saved, attachment);
			saved = assignmentRepository.save(saved);
			activityService.record(professorId, "UPLOADED", "ASSIGNMENT", String.valueOf(saved.getId()),
					saved.getTitle(), "Uploaded assignment attachment.");
		}
		activityService.record(professorId, "CREATED", "ASSIGNMENT", String.valueOf(saved.getId()),
				saved.getTitle(), "Created assignment.");
		return toResponse(saved);
	}

	@Transactional
	public AssignmentResponse update(
			Long id,
			String professorId,
			String professorName,
			AssignmentRequest request,
			MultipartFile attachment
	) {
		Assignment assignment = assignmentRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Assignment not found."));
		if (!assignment.getProfessorId().equals(professorId)) {
			throw new IllegalArgumentException("Unauthorized assignment update.");
		}
		applyRequest(assignment, professorId, professorName, request);
		if (attachment != null && !attachment.isEmpty()) {
			deleteExistingAttachment(assignment);
			storeAttachment(assignment, attachment);
			activityService.record(professorId, "UPLOADED", "ASSIGNMENT", String.valueOf(assignment.getId()),
					assignment.getTitle(), "Uploaded assignment attachment.");
		}
		Assignment saved = assignmentRepository.save(assignment);
		activityService.record(professorId, "UPDATED", "ASSIGNMENT", String.valueOf(saved.getId()),
				saved.getTitle(), "Updated assignment.");
		return toResponse(saved);
	}

	@Transactional(readOnly = true)
	public Assignment getForProfessor(Long id, String professorId) {
		Assignment assignment = assignmentRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Assignment not found."));
		if (!assignment.getProfessorId().equals(professorId)) {
			throw new IllegalArgumentException("Unauthorized assignment access.");
		}
		return assignment;
	}

	@Transactional
	public void delete(Long id, String professorId) {
		Assignment assignment = assignmentRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Assignment not found."));
		if (!assignment.getProfessorId().equals(professorId)) {
			throw new IllegalArgumentException("Unauthorized assignment delete.");
		}
		String title = assignment.getTitle();
		deleteExistingAttachment(assignment);
		assignmentRepository.delete(assignment);
		activityService.record(professorId, "DELETED", "ASSIGNMENT", String.valueOf(id),
				title != null ? title : "Assignment", "Deleted assignment.");
	}

	private void applyRequest(
			Assignment assignment,
			String professorId,
			String professorName,
			AssignmentRequest request
	) {
		String title = requireText(request.getTitle(), "Title is required.");
		String description = requireText(request.getDescription(), "Description is required.");
		String subject = requireText(request.getSubject(), "Subject is required.");
		List<String> sections = normalizeSections(request.getSections());
		if (sections.isEmpty()) {
			throw new IllegalArgumentException("At least one section is required.");
		}
		String status = requireText(request.getStatus(), "Status is required.");
		Integer points = request.getPoints();
		if (points != null) {
			if (points < 0 || points > 100) {
				throw new IllegalArgumentException("Points must be between 0 and 100.");
			}
		}

		assignment.setTitle(title);
		assignment.setDescription(description);
		assignment.setSubject(subject);
		assignment.setSectionsCsv(String.join(", ", sections));
		assignment.setStatus(status.toUpperCase());
		assignment.setDueDate(trimToNull(request.getDueDate()));
		assignment.setPoints(points);
		assignment.setProfessorId(professorId);
		assignment.setProfessorName(professorName);
	}

	private AssignmentResponse toResponse(Assignment assignment) {
		AssignmentResponse response = new AssignmentResponse();
		response.setId(assignment.getId());
		response.setTitle(assignment.getTitle());
		response.setDescription(assignment.getDescription());
		response.setSubject(assignment.getSubject());
		response.setSections(splitSections(assignment.getSectionsCsv()));
		response.setStatus(assignment.getStatus());
		response.setDueDate(assignment.getDueDate());
		response.setPoints(assignment.getPoints());
		response.setProfessorId(assignment.getProfessorId());
		response.setProfessorName(assignment.getProfessorName());
		response.setAttachmentName(assignment.getAttachmentName());
		response.setCreatedAt(assignment.getCreatedAt());
		response.setUpdatedAt(assignment.getUpdatedAt());
		return response;
	}

	private void storeAttachment(Assignment assignment, MultipartFile attachment) {
		validateAttachment(attachment);
		try {
			Files.createDirectories(attachmentRoot);
			String originalName = attachment.getOriginalFilename();
			String extension = extractExtension(originalName);
			String filename = "assignment-" + assignment.getId() + "-" + UUID.randomUUID() + "." + extension;
			Path target = attachmentRoot.resolve(filename).normalize();
			try (InputStream inputStream = attachment.getInputStream()) {
				Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
			}
			assignment.setAttachmentName(originalName);
			assignment.setAttachmentPath(target.toString());
			assignment.setAttachmentContentType(attachment.getContentType());
			assignment.setAttachmentSize(attachment.getSize());
		} catch (IOException ex) {
			throw new IllegalStateException("Unable to store attachment.", ex);
		}
	}

	private void deleteExistingAttachment(Assignment assignment) {
		if (assignment.getAttachmentPath() == null || assignment.getAttachmentPath().isBlank()) {
			return;
		}
		try {
			Files.deleteIfExists(Path.of(assignment.getAttachmentPath()));
		} catch (IOException ex) {
			throw new IllegalStateException("Unable to replace existing attachment.", ex);
		}
		assignment.setAttachmentPath(null);
		assignment.setAttachmentName(null);
		assignment.setAttachmentContentType(null);
		assignment.setAttachmentSize(null);
	}

	private static void validateAttachment(MultipartFile attachment) {
		String originalName = attachment.getOriginalFilename();
		String extension = extractExtension(originalName);
		String contentType = attachment.getContentType();

		boolean extensionAllowed = extension != null && ALLOWED_EXTENSIONS.contains(extension.toLowerCase());
		boolean contentTypeAllowed = contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType);

		if (!extensionAllowed && !contentTypeAllowed) {
			throw new IllegalArgumentException("Unsupported attachment type. Allowed: PDF, DOC, DOCX, PPT, PPTX.");
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

	private static String requireText(String value, String message) {
		String trimmed = trimToNull(value);
		if (trimmed == null) {
			throw new IllegalArgumentException(message);
		}
		return trimmed;
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static List<String> normalizeSections(List<String> sections) {
		if (sections == null) {
			return List.of();
		}
		LinkedHashSet<String> unique = new LinkedHashSet<>();
		for (String section : sections) {
			if (section == null) continue;
			String trimmed = section.trim();
			if (!trimmed.isEmpty()) {
				unique.add(trimmed);
			}
		}
		return new ArrayList<>(unique);
	}

	private static List<String> splitSections(String csv) {
		if (csv == null || csv.isBlank()) {
			return List.of();
		}
		String[] parts = csv.split(",");
		List<String> sections = new ArrayList<>();
		for (String part : parts) {
			String trimmed = part.trim();
			if (!trimmed.isEmpty()) {
				sections.add(trimmed);
			}
		}
		return sections;
	}
}
