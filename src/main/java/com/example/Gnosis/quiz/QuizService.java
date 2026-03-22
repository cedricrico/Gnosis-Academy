package com.example.Gnosis.quiz;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Gnosis.activity.ProfessorActivityService;

import java.util.List;
import java.util.NoSuchElementException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
public class QuizService {
	private static final int MAX_ALLOWED_QUIZ_ATTEMPTS = 3;
	private final QuizRepository quizRepository;
	private final ProfessorActivityService activityService;

	public QuizService(QuizRepository quizRepository, ProfessorActivityService activityService) {
		this.quizRepository = quizRepository;
		this.activityService = activityService;
	}

	@Transactional(readOnly = true)
	public List<QuizResponse> listForProfessor(String professorId) {
		return quizRepository.findByProfessorIdOrderByCreatedAtDesc(professorId)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<QuizResponse> listForStudentSection(String studentSection, java.util.Set<String> allowedSubjects) {
		java.util.Set<String> normalizedSubjects = normalizeSubjects(allowedSubjects);
		return quizRepository.findAllByOrderByCreatedAtDesc()
				.stream()
				.filter(quiz -> isSectionAllowed(quiz.getSection(), studentSection))
				.filter(quiz -> normalizedSubjects.isEmpty() || subjectAllowed(quiz.getSubject(), normalizedSubjects))
				.filter(QuizService::statusAllowedForStudent)
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public QuizResponse getForStudent(Long id, String studentSection, java.util.Set<String> allowedSubjects) {
		return toResponse(getEntityForStudent(id, studentSection, allowedSubjects));
	}

	@Transactional(readOnly = true)
	public Quiz getEntityForStudent(Long id, String studentSection, java.util.Set<String> allowedSubjects) {
		Quiz quiz = quizRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Quiz not found."));
		if (!isSectionAllowed(quiz.getSection(), studentSection)) {
			throw new IllegalArgumentException("Unauthorized quiz access.");
		}
		java.util.Set<String> normalizedSubjects = normalizeSubjects(allowedSubjects);
		if (!normalizedSubjects.isEmpty() && !subjectAllowed(quiz.getSubject(), normalizedSubjects)) {
			throw new IllegalArgumentException("Unauthorized quiz access.");
		}
		if (!statusAllowedForStudent(quiz)) {
			throw new IllegalArgumentException("Unauthorized quiz access.");
		}
		if (isExpired(quiz)) {
			throw new IllegalArgumentException("Quiz has already expired.");
		}
		return quiz;
	}

	@Transactional(readOnly = true)
	public Quiz getEntityForStudentByCode(String code, String studentSection, java.util.Set<String> allowedSubjects) {
		String normalizedCode = trimToNull(code);
		if (normalizedCode == null) {
			throw new IllegalArgumentException("Quiz code is required.");
		}
		java.util.Set<String> normalizedSubjects = normalizeSubjects(allowedSubjects);
		return quizRepository.findByCodeIgnoreCaseOrderByCreatedAtDesc(normalizedCode)
				.stream()
				.filter(quiz -> isSectionAllowed(quiz.getSection(), studentSection))
				.filter(quiz -> normalizedSubjects.isEmpty() || subjectAllowed(quiz.getSubject(), normalizedSubjects))
				.filter(QuizService::statusAllowedForStudent)
				.filter(quiz -> !isExpired(quiz))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Quiz code not found or unavailable for your class."));
	}

	@Transactional
	public QuizResponse create(String professorId, String professorName, QuizRequest request) {
		Quiz quiz = new Quiz();
		applyRequest(quiz, professorId, professorName, request);
		Quiz saved = quizRepository.save(quiz);
		activityService.record(professorId, "CREATED", "QUIZ", String.valueOf(saved.getId()),
				saved.getTitle(), "Created quiz.");
		return toResponse(saved);
	}

	@Transactional
	public QuizResponse update(Long id, String professorId, String professorName, QuizRequest request) {
		Quiz quiz = quizRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Quiz not found."));
		if (!quiz.getProfessorId().equals(professorId)) {
			throw new IllegalArgumentException("Unauthorized quiz update.");
		}
		applyRequest(quiz, professorId, professorName, request);
		Quiz saved = quizRepository.save(quiz);
		activityService.record(professorId, "UPDATED", "QUIZ", String.valueOf(saved.getId()),
				saved.getTitle(), "Updated quiz.");
		return toResponse(saved);
	}

	@Transactional
	public void delete(Long id, String professorId) {
		Quiz quiz = quizRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Quiz not found."));
		if (!quiz.getProfessorId().equals(professorId)) {
			throw new IllegalArgumentException("Unauthorized quiz delete.");
		}
		String title = quiz.getTitle();
		quizRepository.delete(quiz);
		activityService.record(professorId, "DELETED", "QUIZ", String.valueOf(id),
				title != null ? title : "Quiz", "Deleted quiz.");
	}

	private void applyRequest(Quiz quiz, String professorId, String professorName, QuizRequest request) {
		String title = requireText(request.getTitle(), "Title is required.");
		String code = requireText(request.getCode(), "Code is required.");
		String subject = requireText(request.getSubject(), "Subject is required.");
		String status = requireText(request.getStatus(), "Status is required.");
		Integer durationMinutes = request.getDurationMinutes();
		Integer questionCount = request.getQuestionCount();
		if (durationMinutes == null || durationMinutes < 1) {
			throw new IllegalArgumentException("Duration must be at least 1 minute.");
		}
		if (questionCount == null || questionCount < 1) {
			throw new IllegalArgumentException("At least one question is required.");
		}
		Integer attempts = request.getAttempts();
		if (attempts != null && (attempts < 1 || attempts > MAX_ALLOWED_QUIZ_ATTEMPTS)) {
			throw new IllegalArgumentException("Allowed attempts must be between 1 and 3.");
		}

		quiz.setTitle(title);
		quiz.setCode(code);
		quiz.setSubject(subject);
		quiz.setSection(trimToNull(request.getSection()));
		quiz.setDurationMinutes(durationMinutes);
		quiz.setQuestionCount(questionCount);
		quiz.setAttempts(attempts);
		quiz.setDescription(trimToNull(request.getDescription()));
		quiz.setDueDate(trimToNull(request.getDueDate()));
		String questionsJson = trimToNull(request.getQuestionsJson());
		if (questionsJson == null) {
			questionsJson = quiz.getQuestionsJson();
		}
		if (questionsJson == null) {
			questionsJson = "[]";
		}
		quiz.setQuestionsJson(questionsJson);
		// If a professor saves a quiz with status EXPIRED but a future due date,
		// treat it as PUBLISHED — the quiz is no longer actually expired.
		String resolvedStatus = status.toUpperCase();
		if ("EXPIRED".equals(resolvedStatus)) {
			String dueDateValue = trimToNull(request.getDueDate());
			if (dueDateValue != null) {
				LocalDateTime dt = parseDueDate(dueDateValue);
				if (dt != null && dt.isAfter(LocalDateTime.now())) {
					resolvedStatus = "PUBLISHED";
				}
			}
		}
		quiz.setStatus(resolvedStatus);
		quiz.setProfessorId(professorId);
		quiz.setProfessorName(professorName);
	}

	private QuizResponse toResponse(Quiz quiz) {
		QuizResponse response = new QuizResponse();
		response.setId(quiz.getId());
		response.setTitle(quiz.getTitle());
		response.setCode(quiz.getCode());
		response.setSubject(quiz.getSubject());
		response.setSection(quiz.getSection());
		response.setDurationMinutes(quiz.getDurationMinutes());
		response.setQuestionCount(quiz.getQuestionCount());
		response.setAttempts(quiz.getAttempts());
		response.setStatus(resolveDisplayStatus(quiz));
		response.setDueDate(quiz.getDueDate());
		response.setDescription(quiz.getDescription());
		response.setQuestionsJson(quiz.getQuestionsJson());
		response.setProfessorId(quiz.getProfessorId());
		response.setProfessorName(quiz.getProfessorName());
		response.setCreatedAt(quiz.getCreatedAt());
		response.setUpdatedAt(quiz.getUpdatedAt());
		return response;
	}

	private static boolean isSectionAllowed(String quizSection, String studentSection) {
		if (quizSection == null || quizSection.isBlank()) {
			return true;
		}
		String normalized = quizSection.trim();
		if ("All Sections".equalsIgnoreCase(normalized)) {
			return true;
		}
		if (studentSection == null || studentSection.isBlank()) {
			return false;
		}
		String studentNormalized = studentSection.trim();
		if (normalized.equalsIgnoreCase(studentNormalized)) {
			return true;
		}
		String compactQuizSection = normalizeLooseLabel(normalized);
		String compactStudentSection = normalizeLooseLabel(studentNormalized);
		return compactQuizSection.equals(compactStudentSection)
				|| compactQuizSection.endsWith(compactStudentSection)
				|| compactStudentSection.endsWith(compactQuizSection);
	}

	private static boolean subjectAllowed(String subject, java.util.Set<String> allowedSubjects) {
		if (subject == null || allowedSubjects == null || allowedSubjects.isEmpty()) {
			return false;
		}
		for (String variant : normalizeSubjectVariants(subject)) {
			if (allowedSubjects.contains(variant)) {
				return true;
			}
			String compactVariant = normalizeLooseLabel(variant);
			for (String allowed : allowedSubjects) {
				if (allowed == null || allowed.isBlank()) {
					continue;
				}
				String compactAllowed = normalizeLooseLabel(allowed);
				if (compactVariant.equals(compactAllowed)
						|| compactVariant.contains(compactAllowed)
						|| compactAllowed.contains(compactVariant)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean statusAllowedForStudent(Quiz quiz) {
		if (quiz == null) {
			return false;
		}
		String status = quiz.getStatus();
		if (status == null) {
			return true;
		}
		return !"draft".equalsIgnoreCase(status.trim());
	}

	public static boolean isExpired(Quiz quiz) {
		if (quiz == null) {
			return false;
		}
		String dueDate = trimToNull(quiz.getDueDate());
		if (dueDate == null) {
			return false;
		}
		LocalDateTime dt = parseDueDate(dueDate);
		return dt != null && dt.isBefore(LocalDateTime.now());
	}

	/** Parses a due-date string that may or may not include seconds
	 *  (HTML datetime-local inputs omit seconds). */
	private static LocalDateTime parseDueDate(String value) {
		if (value == null) return null;
		try {
			return LocalDateTime.parse(value);
		} catch (DateTimeParseException ignored) {}
		try {
			return LocalDateTime.parse(value,
					DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
		} catch (DateTimeParseException ignored) {}
		return null;
	}

	private static String resolveDisplayStatus(Quiz quiz) {
		if (quiz == null) {
			return null;
		}
		String status = trimToNull(quiz.getStatus());
		if (status == null) {
			return isExpired(quiz) ? "EXPIRED" : null;
		}
		// Stored as EXPIRED but due date is in the future → treat as PUBLISHED
		// (handles rows saved before this fix was applied)
		if ("expired".equalsIgnoreCase(status) && !isExpired(quiz)) {
			return "PUBLISHED";
		}
		if (!"archived".equalsIgnoreCase(status) && isExpired(quiz)) {
			return "EXPIRED";
		}
		return status;
	}

	private static java.util.Set<String> normalizeSubjects(java.util.Set<String> allowedSubjects) {
		if (allowedSubjects == null || allowedSubjects.isEmpty()) {
			return java.util.Set.of();
		}
		return allowedSubjects.stream()
				.flatMap(subject -> normalizeSubjectVariants(subject).stream())
				.collect(java.util.stream.Collectors.toSet());
	}

	private static java.util.Set<String> normalizeSubjectVariants(String subject) {
		if (subject == null) {
			return java.util.Set.of();
		}
		String normalized = subject.trim().toLowerCase();
		if (normalized.isBlank()) {
			return java.util.Set.of();
		}
		java.util.LinkedHashSet<String> variants = new java.util.LinkedHashSet<>();
		variants.add(normalized);
		if (normalized.contains(" - ")) {
			String[] parts = normalized.split("\\s+-\\s+", 2);
			for (String part : parts) {
				if (!part.isBlank()) {
					variants.add(part.trim());
				}
			}
		}
		return variants;
	}

	private static String requireText(String value, String message) {
		String trimmed = trimToNull(value);
		if (trimmed == null) {
			throw new IllegalArgumentException(message);
		}
		return trimmed;
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static String normalizeLooseLabel(String value) {
		if (value == null) {
			return "";
		}
		return value.trim()
				.toLowerCase()
				.replace('-', ' ')
				.replace('_', ' ')
				.replace('/', ' ')
				.replaceAll("\\s+", " ");
	}
}
