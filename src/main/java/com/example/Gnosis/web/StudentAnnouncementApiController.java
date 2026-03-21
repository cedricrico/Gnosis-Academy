package com.example.Gnosis.web;

import com.example.Gnosis.announcement.Announcement;
import com.example.Gnosis.announcement.AnnouncementService;
import com.example.Gnosis.user.User;
import com.example.Gnosis.user.UserRepository;
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

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/student/api/announcements")
public class StudentAnnouncementApiController {
	private final AnnouncementService announcementService;
	private final UserRepository userRepository;

	public StudentAnnouncementApiController(AnnouncementService announcementService, UserRepository userRepository) {
		this.announcementService = announcementService;
		this.userRepository = userRepository;
	}

	@GetMapping("/{id}/image")
	public ResponseEntity<Resource> image(@PathVariable Long id, Authentication authentication) {
		User student = resolveStudent(authentication);
		Announcement announcement = announcementService.getForStudent(id, student.getSectionName());
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

	private User resolveStudent(Authentication authentication) {
		if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
			throw new IllegalArgumentException("Authentication required.");
		}
		return userRepository.findByStudentId(authentication.getName())
				.orElseThrow(() -> new NoSuchElementException("Student not found."));
	}
}
