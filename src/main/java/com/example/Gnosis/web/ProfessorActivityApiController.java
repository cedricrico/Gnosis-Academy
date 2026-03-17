package com.example.Gnosis.web;

import com.example.Gnosis.activity.ProfessorActivity;
import com.example.Gnosis.activity.ProfessorActivityService;
import com.example.Gnosis.security.ProfessorUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/professor/api/activity")
public class ProfessorActivityApiController {
	private final ProfessorActivityService professorActivityService;

	public ProfessorActivityApiController(ProfessorActivityService professorActivityService) {
		this.professorActivityService = professorActivityService;
	}

	@GetMapping
	public List<ActivityResponse> listRecent(
			Authentication authentication,
			@RequestParam(name = "limit", defaultValue = "8") int limit
	) {
		String professorId = resolveProfessorId(authentication);
		return professorActivityService.listRecent(professorId, Math.max(1, Math.min(20, limit)))
				.stream()
				.map(ProfessorActivityApiController::toResponse)
				.toList();
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(
			@PathVariable Long id,
			Authentication authentication
	) {
		String professorId = resolveProfessorId(authentication);
		professorActivityService.delete(id, professorId);
		return ResponseEntity.noContent().build();
	}

	private static String resolveProfessorId(Authentication authentication) {
		if (authentication == null) {
			return "";
		}
		String professorId = authentication.getName();
		if (authentication.getPrincipal() instanceof ProfessorUserDetails pud) {
			professorId = pud.getEmployeeId();
		}
		return professorId;
	}

	private static ActivityResponse toResponse(ProfessorActivity activity) {
		return new ActivityResponse(
				activity.getId(),
				activity.getAction(),
				activity.getEntityType(),
				activity.getEntityId(),
				activity.getTitle(),
				activity.getDetails(),
				activity.getCreatedAt().toString()
		);
	}

	private record ActivityResponse(
			Long id,
			String action,
			String entityType,
			String entityId,
			String title,
			String details,
			String createdAt
	) {}
}
