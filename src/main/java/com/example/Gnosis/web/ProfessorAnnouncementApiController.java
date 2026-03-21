package com.example.Gnosis.web;

import com.example.Gnosis.announcement.AnnouncementRequest;
import com.example.Gnosis.announcement.AnnouncementResponse;
import com.example.Gnosis.announcement.AnnouncementService;
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
@RequestMapping("/professor/api/announcements")
public class ProfessorAnnouncementApiController {
	private final AnnouncementService announcementService;

	public ProfessorAnnouncementApiController(AnnouncementService announcementService) {
		this.announcementService = announcementService;
	}

	@GetMapping
	public List<AnnouncementResponse> list(Authentication authentication) {
		ProfessorIdentity professor = resolveProfessor(authentication);
		return announcementService.listForProfessor(professor.id())
				.stream()
				.peek(this::applyProfessorImageUrl)
				.toList();
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<AnnouncementResponse> create(
			Authentication authentication,
			@RequestParam String title,
			@RequestParam String content,
			@RequestParam String subject,
			@RequestParam String status,
			@RequestParam(required = false) String postedOn,
			@RequestPart(name = "image", required = false) MultipartFile image
	) {
		AnnouncementRequest request = new AnnouncementRequest();
		request.setTitle(title);
		request.setContent(content);
		request.setSubject(subject);
		request.setStatus(status);
		request.setPostedOn(postedOn);
		ProfessorIdentity professor = resolveProfessor(authentication);
		AnnouncementResponse created = announcementService.create(professor.id(), professor.name(), request, image);
		applyProfessorImageUrl(created);
		return ResponseEntity.ok(created);
	}

	@PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<AnnouncementResponse> update(
			@PathVariable Long id,
			Authentication authentication,
			@RequestParam String title,
			@RequestParam String content,
			@RequestParam String subject,
			@RequestParam String status,
			@RequestParam(required = false) String postedOn,
			@RequestPart(name = "image", required = false) MultipartFile image
	) {
		AnnouncementRequest request = new AnnouncementRequest();
		request.setTitle(title);
		request.setContent(content);
		request.setSubject(subject);
		request.setStatus(status);
		request.setPostedOn(postedOn);
		ProfessorIdentity professor = resolveProfessor(authentication);
		AnnouncementResponse updated = announcementService.update(id, professor.id(), professor.name(), request, image);
		applyProfessorImageUrl(updated);
		return ResponseEntity.ok(updated);
	}

	@GetMapping("/{id}/image")
	public ResponseEntity<Resource> image(
			@PathVariable Long id,
			Authentication authentication
	) {
		ProfessorIdentity professor = resolveProfessor(authentication);
		com.example.Gnosis.announcement.Announcement announcement = announcementService.getForProfessor(id, professor.id());
		if (announcement.getImagePath() == null || announcement.getImagePath().isBlank()) {
			return ResponseEntity.notFound().build();
		}
		Resource resource = new FileSystemResource(announcement.getImagePath());
		if (!resource.exists()) {
			return ResponseEntity.notFound().build();
		}
		String contentType = announcement.getImageContentType() != null
				? announcement.getImageContentType()
				: MediaType.APPLICATION_OCTET_STREAM_VALUE;
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(contentType))
				.header(HttpHeaders.CONTENT_DISPOSITION,
						"inline; filename=\"" + announcement.getImageName() + "\"")
				.body(resource);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(
			@PathVariable Long id,
			Authentication authentication
	) {
		ProfessorIdentity professor = resolveProfessor(authentication);
		announcementService.delete(id, professor.id());
		return ResponseEntity.noContent().build();
	}

	private static ProfessorIdentity resolveProfessor(Authentication authentication) {
		if (authentication != null && authentication.getPrincipal() instanceof com.example.Gnosis.security.ProfessorUserDetails pud) {
			return new ProfessorIdentity(pud.getEmployeeId(), pud.getFullName());
		}
		String fallback = authentication != null ? authentication.getName() : "unknown";
		return new ProfessorIdentity(fallback, fallback);
	}

	private void applyProfessorImageUrl(AnnouncementResponse response) {
		if (response != null && response.getImageName() != null && !response.getImageName().isBlank()) {
			response.setImageUrl("/professor/api/announcements/" + response.getId() + "/image");
		}
	}

	private record ProfessorIdentity(String id, String name) {}
}
