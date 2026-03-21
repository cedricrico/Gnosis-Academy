package com.example.Gnosis.web;

import com.example.Gnosis.quiz.QuizResponse;
import com.example.Gnosis.quiz.QuizAttemptService;
import com.example.Gnosis.quiz.QuizService;
import com.example.Gnosis.schoolclass.SchoolClassDto;
import com.example.Gnosis.schoolclass.SchoolClassService;
import com.example.Gnosis.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/student/api/quizzes")
public class StudentQuizApiController {
	private final QuizService quizService;
	private final QuizAttemptService quizAttemptService;
	private final UserRepository userRepository;
	private final SchoolClassService schoolClassService;

	public StudentQuizApiController(
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

	@GetMapping
	public List<QuizResponse> list(Authentication authentication) {
		StudentContext context = resolveStudentContext(authentication);
		String studentId = requireStudentId(authentication);
		Set<String> allowedSubjects = resolveAllowedSubjects(context);
		return quizService.listForStudentSection(context.section(), allowedSubjects).stream()
				.peek(quiz -> {
					long attemptsUsed = quizAttemptService.countAttempts(quiz.getId(), studentId);
					QuizAttemptService.LatestQuizAttemptSummary latestAttempt =
							quizAttemptService.latestAttemptSummary(quiz.getId(), studentId);
					quiz.setAttemptsUsed(Math.toIntExact(attemptsUsed));
					if (quiz.getAttempts() != null && quiz.getAttempts() > 0) {
						quiz.setAttemptsRemaining(Math.max(quiz.getAttempts() - Math.toIntExact(attemptsUsed), 0));
						quiz.setCanAttempt(attemptsUsed < quiz.getAttempts());
					} else {
						quiz.setAttemptsRemaining(null);
						quiz.setCanAttempt(true);
					}
					if (isExpired(quiz.getDueDate())) {
						quiz.setCanAttempt(false);
					}
					if (latestAttempt != null) {
						quiz.setLatestScore(latestAttempt.correctAnswers());
						quiz.setLatestScoreTotal(latestAttempt.totalQuestions());
					}
					quiz.setQuestionsJson(null);
				})
				.toList();
	}

	@GetMapping("/{id}/session")
	public QuizAttemptService.StudentQuizSession session(
			@PathVariable Long id,
			Authentication authentication
	) {
		StudentContext context = resolveStudentContext(authentication);
		com.example.Gnosis.quiz.Quiz quiz = quizService.getEntityForStudent(id, context.section(), resolveAllowedSubjects(context));
		String studentId = requireStudentId(authentication);
		return quizAttemptService.buildSession(quiz, studentId);
	}

	@PostMapping("/{id}/submit")
	public QuizAttemptService.QuizSubmissionResult submit(
			@PathVariable Long id,
			Authentication authentication,
			@RequestBody QuizSubmissionRequest request
	) {
		StudentContext context = resolveStudentContext(authentication);
		com.example.Gnosis.quiz.Quiz quiz = quizService.getEntityForStudent(id, context.section(), resolveAllowedSubjects(context));
		String studentId = requireStudentId(authentication);
		return quizAttemptService.submit(
				quiz,
				studentId,
				request.answers() == null ? java.util.Map.of() : request.answers(),
				request.forceSubmit()
		);
	}

	@GetMapping("/{id}/attempts/{attemptNumber}")
	public QuizAttemptService.QuizSubmissionResult attemptResult(
			@PathVariable Long id,
			@PathVariable Integer attemptNumber,
			Authentication authentication
	) {
		StudentContext context = resolveStudentContext(authentication);
		com.example.Gnosis.quiz.Quiz quiz = quizService.getEntityForStudent(id, context.section(), resolveAllowedSubjects(context));
		String studentId = requireStudentId(authentication);
		return quizAttemptService.loadAttemptResult(quiz, studentId, attemptNumber);
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

	private static String requireStudentId(Authentication authentication) {
		if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
			throw new IllegalArgumentException("Student is required.");
		}
		return authentication.getName().trim();
	}

	private static boolean isExpired(String dueDate) {
		String value = normalizeValue(dueDate);
		if (value == null) {
			return false;
		}
		try {
			return java.time.LocalDateTime.parse(value).isBefore(java.time.LocalDateTime.now());
		} catch (java.time.format.DateTimeParseException ignored) {
			return false;
		}
	}

	private record StudentContext(String course, String section) {}
	private record QuizSubmissionRequest(java.util.Map<String, String> answers, boolean forceSubmit) {}
}
