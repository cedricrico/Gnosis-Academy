package com.example.Gnosis.web;

import com.example.Gnosis.lesson.LessonRequest;
import com.example.Gnosis.lesson.LessonResponse;
import com.example.Gnosis.lesson.LessonService;
import com.example.Gnosis.schoolclass.SchoolClassDto;
import com.example.Gnosis.schoolclass.SchoolClassService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@RestController
@RequestMapping("/professor/api/lessons")
public class ProfessorLessonApiController {
	private final LessonService lessonService;
	private final SchoolClassService schoolClassService;

	public ProfessorLessonApiController(LessonService lessonService, SchoolClassService schoolClassService) {
		this.lessonService = lessonService;
		this.schoolClassService = schoolClassService;
	}

	@GetMapping("/meta")
	public LessonMetaResponse meta(Authentication authentication) {
		ProfessorIdentity professor = resolveProfessor(authentication);
		List<SchoolClassDto> classes = schoolClassService.findForProfessor(professor.id(), professor.name());
		LinkedHashSet<String> sections = new LinkedHashSet<>();
		LinkedHashSet<String> subjects = new LinkedHashSet<>();

		for (SchoolClassDto schoolClass : classes) {
			String section = safeTrim(schoolClass.getSectionName());
			if (section != null) {
				sections.add(section);
			}

			for (SchoolClassDto.SubjectDto subject : schoolClass.getSubjects()) {
				String code = safeTrim(subject.getCode());
				String name = safeTrim(subject.getName());
				String label = code != null ? code : name;
				if (code != null && name != null) {
					label = code + " - " + name;
				}
				if (label != null) {
					subjects.add(label);
				}
			}
		}

		return new LessonMetaResponse(new ArrayList<>(sections), new ArrayList<>(subjects));
	}

	@GetMapping
	public List<LessonResponse> list(Authentication authentication) {
		ProfessorIdentity professor = resolveProfessor(authentication);
		return lessonService.listForProfessor(professor.id())
				.stream()
				.peek(response -> {
					if (response.getAttachmentName() != null && !response.getAttachmentName().isBlank()) {
						response.setAttachmentUrl("/professor/api/lessons/" + response.getId() + "/attachment");
					}
				})
				.toList();
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<LessonResponse> create(
			Authentication authentication,
			@RequestParam String title,
			@RequestParam String subject,
			@RequestParam(name = "section", required = false) String section,
			@RequestParam String week,
			@RequestParam String description,
			@RequestParam String type,
			@RequestPart(name = "attachment", required = false) MultipartFile attachment
	) {
		LessonRequest request = new LessonRequest();
		request.setTitle(title);
		request.setSubject(subject);
		request.setSection(section);
		request.setWeek(week);
		request.setDescription(description);
		request.setType(type);
		ProfessorIdentity professor = resolveProfessor(authentication);
		LessonResponse created = lessonService.create(professor.id(), professor.name(), request, attachment);
		if (created.getAttachmentName() != null && !created.getAttachmentName().isBlank()) {
			created.setAttachmentUrl("/professor/api/lessons/" + created.getId() + "/attachment");
		}
		return ResponseEntity.ok(created);
	}

	@PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<LessonResponse> update(
			@PathVariable Long id,
			Authentication authentication,
			@RequestParam String title,
			@RequestParam String subject,
			@RequestParam(name = "section", required = false) String section,
			@RequestParam String week,
			@RequestParam String description,
			@RequestParam String type,
			@RequestPart(name = "attachment", required = false) MultipartFile attachment
	) {
		LessonRequest request = new LessonRequest();
		request.setTitle(title);
		request.setSubject(subject);
		request.setSection(section);
		request.setWeek(week);
		request.setDescription(description);
		request.setType(type);
		ProfessorIdentity professor = resolveProfessor(authentication);
		LessonResponse updated = lessonService.update(id, professor.id(), professor.name(), request, attachment);
		if (updated.getAttachmentName() != null && !updated.getAttachmentName().isBlank()) {
			updated.setAttachmentUrl("/professor/api/lessons/" + updated.getId() + "/attachment");
		}
		return ResponseEntity.ok(updated);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(
			@PathVariable Long id,
			Authentication authentication
	) {
		ProfessorIdentity professor = resolveProfessor(authentication);
		lessonService.delete(id, professor.id());
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}/attachment")
	public ResponseEntity<Resource> downloadAttachment(
			@PathVariable Long id,
			Authentication authentication
	) {
		ProfessorIdentity professor = resolveProfessor(authentication);
		com.example.Gnosis.lesson.Lesson lesson = lessonService.getForProfessor(id, professor.id());
		if (lesson.getAttachmentPath() == null || lesson.getAttachmentPath().isBlank()) {
			return ResponseEntity.notFound().build();
		}
		Resource resource = new FileSystemResource(lesson.getAttachmentPath());
		if (!resource.exists()) {
			return ResponseEntity.notFound().build();
		}
		String contentType = lesson.getAttachmentContentType() != null
				? lesson.getAttachmentContentType()
				: MediaType.APPLICATION_OCTET_STREAM_VALUE;
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(contentType))
				.header(HttpHeaders.CONTENT_DISPOSITION,
						"attachment; filename=\"" + lesson.getAttachmentName() + "\"")
				.body(resource);
	}

	private static ProfessorIdentity resolveProfessor(Authentication authentication) {
		if (authentication != null && authentication.getPrincipal() instanceof com.example.Gnosis.security.ProfessorUserDetails pud) {
			return new ProfessorIdentity(pud.getEmployeeId(), pud.getFullName());
		}
		String fallback = authentication != null ? authentication.getName() : "unknown";
		return new ProfessorIdentity(fallback, fallback);
	}

	private record ProfessorIdentity(String id, String name) {}
	private record LessonMetaResponse(List<String> sections, List<String> subjects) {}

	private static String safeTrim(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
