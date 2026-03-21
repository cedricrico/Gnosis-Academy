package com.example.Gnosis.web;

import com.example.Gnosis.quiz.QuizAttemptService;
import com.example.Gnosis.quiz.QuizService;
import com.example.Gnosis.schoolclass.SchoolClassDto;
import com.example.Gnosis.schoolclass.SchoolClassService;
import com.example.Gnosis.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/quiz-attempts")
public class QuizAttemptApiController {
	private final QuizService quizService;
	private final QuizAttemptService quizAttemptService;
	private final UserRepository userRepository;
	private final SchoolClassService schoolClassService;

	public QuizAttemptApiController(
			QuizService quizService,
			QuizAttemptService quizAttemptService,
			UserRepository userRepository,
			SchoolClassService schoolClassService
	) {
		this.quizService = quizService;
		this.quizAttemptService = quizAttemptService;
		this.userRepository = userRepository;
		this.schoolClassService = schoolClassService;
	}

	@PostMapping
	public QuizAttemptService.QuizSubmissionResult submit(
			Authentication authentication,
			@RequestBody QuizAttemptRequest request
	) {
		if (request == null || request.quizId() == null) {
			throw new IllegalArgumentException("Quiz ID is required.");
		}
		StudentContext context = resolveStudentContext(authentication);
		com.example.Gnosis.quiz.Quiz quiz = quizService.getEntityForStudent(
				request.quizId(),
				context.section(),
				resolveAllowedSubjects(context)
		);
		String studentId = requireStudentId(authentication);
		Map<String, String> answers = request.answers() == null ? Map.of() : request.answers();
		return quizAttemptService.submit(quiz, studentId, answers, request.forceSubmit());
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
		if (trimmed.isEmpty() || "Unassigned".equalsIgnoreCase(trimmed)) {
			return null;
		}
		return trimmed;
	}

	private static String requireStudentId(Authentication authentication) {
		if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
			throw new IllegalArgumentException("Student is required.");
		}
		return authentication.getName().trim();
	}

	private record StudentContext(String course, String section) {}
	private record QuizAttemptRequest(Long quizId, Map<String, String> answers, boolean forceSubmit) {}
}
