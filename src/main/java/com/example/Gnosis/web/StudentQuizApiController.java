package com.example.Gnosis.web;

import com.example.Gnosis.quiz.QuizResponse;
import com.example.Gnosis.quiz.QuizService;
import com.example.Gnosis.schoolclass.SchoolClassDto;
import com.example.Gnosis.schoolclass.SchoolClassService;
import com.example.Gnosis.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/student/api/quizzes")
public class StudentQuizApiController {
	private final QuizService quizService;
	private final UserRepository userRepository;
	private final SchoolClassService schoolClassService;

	public StudentQuizApiController(
			QuizService quizService,
			UserRepository userRepository,
			SchoolClassService schoolClassService
	) {
		this.quizService = quizService;
		this.userRepository = userRepository;
		this.schoolClassService = schoolClassService;
	}

	@GetMapping
	public List<QuizResponse> list(Authentication authentication) {
		StudentContext context = resolveStudentContext(authentication);
		Set<String> allowedSubjects = resolveAllowedSubjects(context);
		return quizService.listForStudentSection(context.section(), allowedSubjects);
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
