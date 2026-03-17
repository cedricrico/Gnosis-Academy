package com.example.Gnosis.announcement;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Gnosis.activity.ProfessorActivityService;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class AnnouncementService {
	private final AnnouncementRepository announcementRepository;
	private final ProfessorActivityService activityService;

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
	public AnnouncementResponse create(String professorId, String professorName, AnnouncementRequest request) {
		Announcement announcement = new Announcement();
		applyRequest(announcement, professorId, professorName, request);
		Announcement saved = announcementRepository.save(announcement);
		activityService.record(professorId, "CREATED", "ANNOUNCEMENT", String.valueOf(saved.getId()),
				saved.getTitle(), "Created announcement.");
		return toResponse(saved);
	}

	@Transactional
	public AnnouncementResponse update(Long id, String professorId, String professorName, AnnouncementRequest request) {
		Announcement announcement = announcementRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Announcement not found."));
		if (!announcement.getProfessorId().equals(professorId)) {
			throw new IllegalArgumentException("Unauthorized announcement update.");
		}
		applyRequest(announcement, professorId, professorName, request);
		Announcement saved = announcementRepository.save(announcement);
		activityService.record(professorId, "UPDATED", "ANNOUNCEMENT", String.valueOf(saved.getId()),
				saved.getTitle(), "Updated announcement.");
		return toResponse(saved);
	}

	@Transactional
	public void delete(Long id, String professorId) {
		Announcement announcement = announcementRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Announcement not found."));
		if (!announcement.getProfessorId().equals(professorId)) {
			throw new IllegalArgumentException("Unauthorized announcement delete.");
		}
		String title = announcement.getTitle();
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
		response.setCreatedAt(announcement.getCreatedAt());
		response.setUpdatedAt(announcement.getUpdatedAt());
		return response;
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
