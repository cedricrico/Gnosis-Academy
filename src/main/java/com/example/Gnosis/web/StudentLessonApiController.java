package com.example.Gnosis.web;

import com.example.Gnosis.lesson.LessonResponse;
import com.example.Gnosis.lesson.LessonService;
import com.example.Gnosis.schoolclass.SchoolClassDto;
import com.example.Gnosis.schoolclass.SchoolClassService;
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

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

@RestController
@RequestMapping("/student/api/lessons")
public class StudentLessonApiController {
	private final LessonService lessonService;
	private final UserRepository userRepository;
	private final SchoolClassService schoolClassService;

	public StudentLessonApiController(
			LessonService lessonService,
			UserRepository userRepository,
			SchoolClassService schoolClassService
	) {
		this.lessonService = lessonService;
		this.userRepository = userRepository;
		this.schoolClassService = schoolClassService;
	}

	@GetMapping
	public List<LessonResponse> list(Authentication authentication) {
		StudentContext context = resolveStudentContext(authentication);
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
		StudentContext context = resolveStudentContext(authentication);
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

	private StudentContext resolveStudentContext(Authentication authentication) {
		if (authentication == null) {
			return new StudentContext(null, null);
		}
		String studentId = authentication.getName();
		if (studentId == null || studentId.isBlank()) {
			return new StudentContext(null, null);
		}
		return userRepository.findByStudentId(studentId)
				.map(user -> new StudentContext(normalizeValue(user.getCourse()), normalizeValue(user.getSectionName())))
				.orElse(new StudentContext(null, null));
	}

	private Set<String> resolveAllowedSubjects(StudentContext context) {
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

	private record StudentContext(String course, String section) {}
}
