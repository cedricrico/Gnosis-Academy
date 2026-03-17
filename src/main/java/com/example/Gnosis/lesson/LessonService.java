package com.example.Gnosis.lesson;

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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

@Service
public class LessonService {
	private final LessonRepository lessonRepository;
	private final ProfessorActivityService activityService;
	private final Path attachmentRoot = Paths.get("uploads", "lessons");
	private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx", "ppt", "pptx", "mp4");
	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
			"application/pdf",
			"application/msword",
			"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
			"application/vnd.ms-powerpoint",
			"application/vnd.openxmlformats-officedocument.presentationml.presentation",
			"video/mp4"
	);

	public LessonService(LessonRepository lessonRepository, ProfessorActivityService activityService) {
		this.lessonRepository = lessonRepository;
		this.activityService = activityService;
	}

	@Transactional(readOnly = true)
	public List<LessonResponse> listForProfessor(String professorId) {
		return lessonRepository.findByProfessorIdOrderByCreatedAtDesc(professorId)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	public LessonResponse create(
			String professorId,
			String professorName,
			LessonRequest request,
			MultipartFile attachment
	) {
		Lesson lesson = new Lesson();
		applyRequest(lesson, professorId, professorName, request);
		Lesson saved = lessonRepository.save(lesson);
		if (attachment != null && !attachment.isEmpty()) {
			storeAttachment(saved, attachment);
			saved = lessonRepository.save(saved);
			activityService.record(professorId, "UPLOADED", "LESSON", String.valueOf(saved.getId()),
					saved.getTitle(), "Uploaded lesson attachment.");
		}
		activityService.record(professorId, "CREATED", "LESSON", String.valueOf(saved.getId()),
				saved.getTitle(), "Created lesson.");
		return toResponse(saved);
	}

	@Transactional
	public LessonResponse update(
			Long id,
			String professorId,
			String professorName,
			LessonRequest request,
			MultipartFile attachment
	) {
		Lesson lesson = lessonRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Lesson not found."));
		if (!lesson.getProfessorId().equals(professorId)) {
			throw new IllegalArgumentException("Unauthorized lesson update.");
		}
		applyRequest(lesson, professorId, professorName, request);
		if (attachment != null && !attachment.isEmpty()) {
			deleteExistingAttachment(lesson);
			storeAttachment(lesson, attachment);
			activityService.record(professorId, "UPLOADED", "LESSON", String.valueOf(lesson.getId()),
					lesson.getTitle(), "Uploaded lesson attachment.");
		}
		Lesson saved = lessonRepository.save(lesson);
		activityService.record(professorId, "UPDATED", "LESSON", String.valueOf(saved.getId()),
				saved.getTitle(), "Updated lesson.");
		return toResponse(saved);
	}

	@Transactional(readOnly = true)
	public Lesson getForProfessor(Long id, String professorId) {
		Lesson lesson = lessonRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Lesson not found."));
		if (!lesson.getProfessorId().equals(professorId)) {
			throw new IllegalArgumentException("Unauthorized lesson access.");
		}
		return lesson;
	}

	@Transactional
	public void delete(Long id, String professorId) {
		Lesson lesson = lessonRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Lesson not found."));
		if (!lesson.getProfessorId().equals(professorId)) {
			throw new IllegalArgumentException("Unauthorized lesson delete.");
		}
		String title = lesson.getTitle();
		deleteExistingAttachment(lesson);
		lessonRepository.delete(lesson);
		activityService.record(professorId, "DELETED", "LESSON", String.valueOf(id),
				title != null ? title : "Lesson", "Deleted lesson.");
	}

	private void applyRequest(
			Lesson lesson,
			String professorId,
			String professorName,
			LessonRequest request
	) {
		String title = requireText(request.getTitle(), "Title is required.");
		String description = requireText(request.getDescription(), "Description is required.");
		String subject = requireText(request.getSubject(), "Subject is required.");
		String week = requireText(request.getWeek(), "Week is required.");
		String type = requireText(request.getType(), "Content type is required.");
		String section = trimToNull(request.getSection());
		if (section == null) {
			section = "All Sections";
		}

		lesson.setTitle(title);
		lesson.setDescription(description);
		lesson.setSubject(subject);
		lesson.setSection(section);
		lesson.setWeek(week);
		lesson.setType(type);
		lesson.setProfessorId(professorId);
		lesson.setProfessorName(professorName);
	}

	private LessonResponse toResponse(Lesson lesson) {
		LessonResponse response = new LessonResponse();
		response.setId(lesson.getId());
		response.setTitle(lesson.getTitle());
		response.setDescription(lesson.getDescription());
		response.setSubject(lesson.getSubject());
		response.setSection(lesson.getSection());
		response.setWeek(lesson.getWeek());
		response.setType(lesson.getType());
		response.setProfessorId(lesson.getProfessorId());
		response.setProfessorName(lesson.getProfessorName());
		response.setAttachmentName(lesson.getAttachmentName());
		response.setCreatedAt(lesson.getCreatedAt());
		response.setUpdatedAt(lesson.getUpdatedAt());
		return response;
	}

	private void storeAttachment(Lesson lesson, MultipartFile attachment) {
		validateAttachment(attachment);
		try {
			Files.createDirectories(attachmentRoot);
			String originalName = attachment.getOriginalFilename();
			String extension = extractExtension(originalName);
			String filename = "lesson-" + lesson.getId() + "-" + UUID.randomUUID() + "." + extension;
			Path target = attachmentRoot.resolve(filename).normalize();
			try (InputStream inputStream = attachment.getInputStream()) {
				Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
			}
			lesson.setAttachmentName(originalName);
			lesson.setAttachmentPath(target.toString());
			lesson.setAttachmentContentType(attachment.getContentType());
			lesson.setAttachmentSize(attachment.getSize());
		} catch (IOException ex) {
			throw new IllegalStateException("Unable to store lesson attachment.", ex);
		}
	}

	private void deleteExistingAttachment(Lesson lesson) {
		if (lesson.getAttachmentPath() == null || lesson.getAttachmentPath().isBlank()) {
			return;
		}
		try {
			Files.deleteIfExists(Path.of(lesson.getAttachmentPath()));
		} catch (IOException ex) {
			throw new IllegalStateException("Unable to replace existing attachment.", ex);
		}
		lesson.setAttachmentPath(null);
		lesson.setAttachmentName(null);
		lesson.setAttachmentContentType(null);
		lesson.setAttachmentSize(null);
	}

	private static void validateAttachment(MultipartFile attachment) {
		String originalName = attachment.getOriginalFilename();
		String extension = extractExtension(originalName);
		String contentType = attachment.getContentType();

		boolean extensionAllowed = extension != null && ALLOWED_EXTENSIONS.contains(extension.toLowerCase());
		boolean contentTypeAllowed = contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType);

		if (!extensionAllowed && !contentTypeAllowed) {
			throw new IllegalArgumentException("Unsupported attachment type. Allowed: PDF, DOC, DOCX, PPT, PPTX, MP4.");
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
}
