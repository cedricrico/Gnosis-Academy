package com.example.Gnosis.web;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

@RestController
@RequestMapping("/student/api/lessons")
public class StudentLessonApiController {
	private final LessonService lessonService;
	private final SchoolClassService schoolClassService;
	private final StudentAccessService studentAccessService;

	public StudentLessonApiController(
			LessonService lessonService,
			SchoolClassService schoolClassService,
			StudentAccessService studentAccessService
	) {
		this.lessonService = lessonService;
		this.schoolClassService = schoolClassService;
		this.studentAccessService = studentAccessService;
	}

	@GetMapping
	public List<LessonResponse> list(Authentication authentication) {
		StudentAccessService.StudentAccessContext context = studentAccessService.resolve(authentication);
		if (!context.canAccessClassContent()) {
			return List.of();
		}
		Set<String> allowedSubjects = resolveAllowedSubjects(context);
		return lessonService.listForStudentSection(context.section(), allowedSubjects)
				.stream()
				.peek(response -> {
					if (response.getAttachmentName() != null && !response.getAttachmentName().isBlank()) {
						response.setAttachmentUrl("/student/api/lessons/" + response.getId() + "/attachment");
					}
				})
				.toList();
	}

	@GetMapping("/{id}/attachment")
	public ResponseEntity<Resource> downloadAttachment(
			@PathVariable Long id,
			Authentication authentication
	) {
		StudentAccessService.StudentAccessContext context = studentAccessService.requireActiveEnrollment(authentication);
		Set<String> allowedSubjects = resolveAllowedSubjects(context);
		com.example.Gnosis.lesson.Lesson lesson = lessonService.getForStudent(id, context.section(), allowedSubjects);
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

	private Set<String> resolveAllowedSubjects(StudentAccessService.StudentAccessContext context) {
		if (context.course() == null || context.section() == null) {
			return Set.of();
		}
		List<SchoolClassDto> classes = schoolClassService.findForStudent(context.course(), context.section());
		LinkedHashSet<String> subjects = new LinkedHashSet<>();
		for (SchoolClassDto schoolClass : classes) {
			for (SchoolClassDto.SubjectDto subject : schoolClass.getSubjects()) {
				String code = normalizeValue(subject.getCode());
				String name = normalizeValue(subject.getName());
				if (code != null && name != null) {
					subjects.add(code + " - " + name);
				}
				if (code != null) {
					subjects.add(code);
				}
				if (name != null) {
					subjects.add(name);
				}
			}
		}
		return subjects;
	}

	private static String normalizeValue(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		if ("Unassigned".equalsIgnoreCase(trimmed)) {
			return null;
		}
		return trimmed;
	}

}
