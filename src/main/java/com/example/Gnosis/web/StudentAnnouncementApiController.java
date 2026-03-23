package com.example.Gnosis.web;

import com.example.Gnosis.announcement.Announcement;
import com.example.Gnosis.announcement.AnnouncementService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/student/api/announcements")
public class StudentAnnouncementApiController {
	private final AnnouncementService announcementService;
	private final StudentAccessService studentAccessService;

	public StudentAnnouncementApiController(
			AnnouncementService announcementService,
			StudentAccessService studentAccessService
	) {
		this.announcementService = announcementService;
		this.studentAccessService = studentAccessService;
	}

	@GetMapping("/{id}/image")
	public ResponseEntity<Resource> image(@PathVariable Long id, Authentication authentication) {
		StudentAccessService.StudentAccessContext context = studentAccessService.requireActiveEnrollment(authentication);
		Announcement announcement = announcementService.getForStudent(id, context.section());
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
}
