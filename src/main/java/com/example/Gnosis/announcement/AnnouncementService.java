package com.example.Gnosis.announcement;

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
public class AnnouncementService {
	private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
	private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of(
			"image/jpeg",
			"image/png",
			"image/gif",
			"image/webp"
	);
	private final AnnouncementRepository announcementRepository;
	private final ProfessorActivityService activityService;
	private final Path imageRoot = Paths.get("uploads", "announcements");

	public AnnouncementService(AnnouncementRepository announcementRepository, ProfessorActivityService activityService) {
		this.announcementRepository = announcementRepository;
		this.activityService = activityService;
	}

	@Transactional(readOnly = true)
	public List<AnnouncementResponse> listForProfessor(String professorId) {
		return announcementRepository.findByProfessorIdOrderByCreatedAtDesc(professorId)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<AnnouncementResponse> listForStudentSection(String sectionName) {
		String normalizedSection = normalizeKey(sectionName);
		return announcementRepository.findByStatusIgnoreCaseOrderByCreatedAtDesc("PUBLISHED")
				.stream()
				.filter(announcement -> shouldIncludeForSection(announcement, normalizedSection))
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	public AnnouncementResponse create(
			String professorId,
			String professorName,
			AnnouncementRequest request,
			MultipartFile image
	) {
		Announcement announcement = new Announcement();
		applyRequest(announcement, professorId, professorName, request);
		Announcement saved = announcementRepository.save(announcement);
		if (image != null && !image.isEmpty()) {
			storeImage(saved, image);
			saved = announcementRepository.save(saved);
			activityService.record(professorId, "UPLOADED", "ANNOUNCEMENT", String.valueOf(saved.getId()),
					saved.getTitle(), "Uploaded announcement image.");
		}
		activityService.record(professorId, "CREATED", "ANNOUNCEMENT", String.valueOf(saved.getId()),
				saved.getTitle(), "Created announcement.");
		return toResponse(saved);
	}

	@Transactional
	public AnnouncementResponse update(
			Long id,
			String professorId,
			String professorName,
			AnnouncementRequest request,
			MultipartFile image
	) {
		Announcement announcement = announcementRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Announcement not found."));
		if (!announcement.getProfessorId().equals(professorId)) {
			throw new IllegalArgumentException("Unauthorized announcement update.");
		}
		applyRequest(announcement, professorId, professorName, request);
		if (image != null && !image.isEmpty()) {
			deleteExistingImage(announcement);
			storeImage(announcement, image);
			activityService.record(professorId, "UPLOADED", "ANNOUNCEMENT", String.valueOf(announcement.getId()),
					announcement.getTitle(), "Uploaded announcement image.");
		}
		Announcement saved = announcementRepository.save(announcement);
		activityService.record(professorId, "UPDATED", "ANNOUNCEMENT", String.valueOf(saved.getId()),
				saved.getTitle(), "Updated announcement.");
		return toResponse(saved);
	}

	@Transactional(readOnly = true)
	public Announcement getForProfessor(Long id, String professorId) {
		Announcement announcement = announcementRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Announcement not found."));
		if (!announcement.getProfessorId().equals(professorId)) {
			throw new IllegalArgumentException("Unauthorized announcement access.");
		}
		return announcement;
	}

	@Transactional(readOnly = true)
	public Announcement getForStudent(Long id, String sectionName) {
		Announcement announcement = announcementRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Announcement not found."));
		if (!"PUBLISHED".equalsIgnoreCase(trimToNull(announcement.getStatus()))) {
			throw new IllegalArgumentException("Unauthorized announcement access.");
		}
		if (!shouldIncludeForSection(announcement, normalizeKey(sectionName))) {
			throw new IllegalArgumentException("Unauthorized announcement access.");
		}
		return announcement;
	}

	@Transactional
	public void delete(Long id, String professorId) {
		Announcement announcement = announcementRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Announcement not found."));
		if (!announcement.getProfessorId().equals(professorId)) {
			throw new IllegalArgumentException("Unauthorized announcement delete.");
		}
		String title = announcement.getTitle();
		deleteExistingImage(announcement);
		announcementRepository.delete(announcement);
		activityService.record(professorId, "DELETED", "ANNOUNCEMENT", String.valueOf(id),
				title != null ? title : "Announcement", "Deleted announcement.");
	}

	private void applyRequest(
			Announcement announcement,
			String professorId,
			String professorName,
			AnnouncementRequest request
	) {
		String title = requireText(request.getTitle(), "Title is required.");
		String content = requireText(request.getContent(), "Content is required.");
		String subject = requireText(request.getSubject(), "Subject is required.");
		String status = requireText(request.getStatus(), "Status is required.");

		announcement.setTitle(title);
		announcement.setContent(content);
		announcement.setSubject(subject);
		announcement.setStatus(status.toUpperCase());
		announcement.setProfessorId(professorId);
		announcement.setProfessorName(professorName);
		announcement.setPostedOn(trimToNull(request.getPostedOn()));
	}

	private AnnouncementResponse toResponse(Announcement announcement) {
		AnnouncementResponse response = new AnnouncementResponse();
		response.setId(announcement.getId());
		response.setTitle(announcement.getTitle());
		response.setContent(announcement.getContent());
		response.setSubject(announcement.getSubject());
		response.setStatus(announcement.getStatus());
		response.setProfessorId(announcement.getProfessorId());
		response.setProfessorName(announcement.getProfessorName());
		response.setPostedOn(announcement.getPostedOn());
		response.setImageName(announcement.getImageName());
		response.setImageContentType(announcement.getImageContentType());
		response.setCreatedAt(announcement.getCreatedAt());
		response.setUpdatedAt(announcement.getUpdatedAt());
		return response;
	}

	private void storeImage(Announcement announcement, MultipartFile image) {
		validateImage(image);
		try {
			Files.createDirectories(imageRoot);
			String originalName = image.getOriginalFilename();
			String extension = extractExtension(originalName);
			String filename = "announcement-" + announcement.getId() + "-" + UUID.randomUUID() + "." + extension;
			Path target = imageRoot.resolve(filename).normalize();
			try (InputStream inputStream = image.getInputStream()) {
				Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
			}
			announcement.setImagePath(target.toString());
			announcement.setImageName(originalName);
			announcement.setImageContentType(image.getContentType());
			announcement.setImageSize(image.getSize());
		} catch (IOException ex) {
			throw new IllegalStateException("Unable to store announcement image.", ex);
		}
	}

	private void deleteExistingImage(Announcement announcement) {
		if (announcement.getImagePath() == null || announcement.getImagePath().isBlank()) {
			return;
		}
		try {
			Files.deleteIfExists(Path.of(announcement.getImagePath()));
		} catch (IOException ex) {
			throw new IllegalStateException("Unable to replace existing image.", ex);
		}
		announcement.setImagePath(null);
		announcement.setImageName(null);
		announcement.setImageContentType(null);
		announcement.setImageSize(null);
	}

	private static void validateImage(MultipartFile image) {
		String originalName = image.getOriginalFilename();
		String extension = extractExtension(originalName);
		String contentType = image.getContentType();
		boolean extensionAllowed = extension != null && ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase());
		boolean contentTypeAllowed = contentType != null && ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType);
		if (!extensionAllowed && !contentTypeAllowed) {
			throw new IllegalArgumentException("Unsupported image type. Allowed: JPG, JPEG, PNG, GIF, WEBP.");
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

	private static boolean shouldIncludeForSection(Announcement announcement, String normalizedSection) {
		String subject = trimToNull(announcement.getSubject());
		if (subject == null) {
			return false;
		}
		String normalizedSubject = normalizeKey(subject);
		if (normalizedSubject.equals("all sections") || normalizedSubject.equals("all section")) {
			return true;
		}
		if (normalizedSection.isEmpty()) {
			return false;
		}
		if (normalizedSubject.equals(normalizedSection)) {
			return true;
		}
		return normalizedSubject.endsWith(" " + normalizedSection);
	}

	private static String normalizeKey(String value) {
		if (value == null) {
			return "";
		}
		String trimmed = value.trim();
		if (trimmed.isEmpty()) {
			return "";
		}
		String normalized = trimmed.replace('\\', ' ')
				.replace('/', ' ')
				.replace('-', ' ')
				.replace('_', ' ');
		return normalized.replaceAll("\\s+", " ").toLowerCase();
	}
}
