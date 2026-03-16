package com.example.Gnosis.announcement;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class AnnouncementService {
	private final AnnouncementRepository announcementRepository;

	public AnnouncementService(AnnouncementRepository announcementRepository) {
		this.announcementRepository = announcementRepository;
	}

	@Transactional(readOnly = true)
	public List<AnnouncementResponse> listForProfessor(String professorId) {
		return announcementRepository.findByProfessorIdOrderByCreatedAtDesc(professorId)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	public AnnouncementResponse create(String professorId, String professorName, AnnouncementRequest request) {
		Announcement announcement = new Announcement();
		applyRequest(announcement, professorId, professorName, request);
		return toResponse(announcementRepository.save(announcement));
	}

	@Transactional
	public AnnouncementResponse update(Long id, String professorId, String professorName, AnnouncementRequest request) {
		Announcement announcement = announcementRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Announcement not found."));
		if (!announcement.getProfessorId().equals(professorId)) {
			throw new IllegalArgumentException("Unauthorized announcement update.");
		}
		applyRequest(announcement, professorId, professorName, request);
		return toResponse(announcementRepository.save(announcement));
	}

	@Transactional
	public void delete(Long id, String professorId) {
		Announcement announcement = announcementRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Announcement not found."));
		if (!announcement.getProfessorId().equals(professorId)) {
			throw new IllegalArgumentException("Unauthorized announcement delete.");
		}
		announcementRepository.delete(announcement);
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
}
