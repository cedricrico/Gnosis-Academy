package com.example.Gnosis.web;

import com.example.Gnosis.announcement.AnnouncementRequest;
import com.example.Gnosis.announcement.AnnouncementResponse;
import com.example.Gnosis.announcement.AnnouncementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
		return announcementService.listForProfessor(professor.id());
	}

	@PostMapping
	public ResponseEntity<AnnouncementResponse> create(
			Authentication authentication,
			@RequestBody AnnouncementRequest request
	) {
		ProfessorIdentity professor = resolveProfessor(authentication);
		AnnouncementResponse created = announcementService.create(professor.id(), professor.name(), request);
		return ResponseEntity.ok(created);
	}

	@PutMapping("/{id}")
	public ResponseEntity<AnnouncementResponse> update(
			@PathVariable Long id,
			Authentication authentication,
			@RequestBody AnnouncementRequest request
	) {
		ProfessorIdentity professor = resolveProfessor(authentication);
		AnnouncementResponse updated = announcementService.update(id, professor.id(), professor.name(), request);
		return ResponseEntity.ok(updated);
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

	private record ProfessorIdentity(String id, String name) {}
}
