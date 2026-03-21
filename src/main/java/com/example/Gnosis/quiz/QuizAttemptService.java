package com.example.Gnosis.quiz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.Gnosis.user.User;
import com.example.Gnosis.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

@Service
public class QuizAttemptService {
	private static final TypeReference<List<QuestionDefinition>> QUESTION_LIST_TYPE = new TypeReference<>() {
	};
	private final QuizAttemptRepository quizAttemptRepository;
	private final UserRepository userRepository;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public QuizAttemptService(QuizAttemptRepository quizAttemptRepository, UserRepository userRepository) {
		this.quizAttemptRepository = quizAttemptRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public long countAttempts(Long quizId, String studentId) {
		if (quizId == null || studentId == null || studentId.isBlank()) {
			return 0;
		}
		return quizAttemptRepository.countByQuizIdAndStudentId(quizId, studentId);
	}

	@Transactional(readOnly = true)
	public LatestQuizAttemptSummary latestAttemptSummary(Long quizId, String studentId) {
		return quizAttemptRepository.findLatestAttemptSummary(quizId, studentId)
				.map(attempt -> new LatestQuizAttemptSummary(
						attempt.getAttemptNumber(),
						attempt.getCorrectAnswers(),
						attempt.getTotalQuestions()
				))
				.orElse(null);
	}

	@Transactional(readOnly = true)
	public StudentQuizSession buildSession(Quiz quiz, String studentId) {
		long attemptsUsed = countAttempts(quiz.getId(), studentId);
		int nextAttemptNumber = Math.toIntExact(attemptsUsed) + 1;
		ensureAttemptsAvailable(quiz, attemptsUsed);
		List<QuestionDefinition> randomizedQuestions =
				randomizeQuestions(parseQuestions(quiz.getQuestionsJson()), quiz.getId(), studentId, nextAttemptNumber);
		return new StudentQuizSession(
				quiz.getId(),
				quiz.getTitle(),
				quiz.getSubject(),
				quiz.getDueDate(),
				quiz.getDescription(),
				quiz.getDurationMinutes(),
				quiz.getAttempts(),
				Math.toIntExact(attemptsUsed),
				computeAttemptsRemaining(quiz.getAttempts(), attemptsUsed),
				toStudentQuestions(randomizedQuestions)
		);
	}

	@Transactional
	public QuizSubmissionResult submit(
			Quiz quiz,
			String studentId,
			Map<String, String> submittedAnswers,
			boolean allowBlankAnswers
	) {
		long attemptsUsed = countAttempts(quiz.getId(), studentId);
		int attemptNumber = Math.toIntExact(attemptsUsed) + 1;
		ensureAttemptsAvailable(quiz, attemptsUsed);

		List<QuestionDefinition> randomizedQuestions =
				randomizeQuestions(parseQuestions(quiz.getQuestionsJson()), quiz.getId(), studentId, attemptNumber);
		if (randomizedQuestions.isEmpty()) {
			throw new IllegalArgumentException("Quiz has no questions.");
		}

		Map<String, String> answers = new LinkedHashMap<>();
		List<QuizSubmissionItem> items = new ArrayList<>();
		int correctAnswers = 0;

		for (QuestionDefinition question : randomizedQuestions) {
			String answer = normalize(submittedAnswers.get(question.id()));
			if (!allowBlankAnswers && answer.isBlank()) {
				throw new IllegalArgumentException("Please answer all questions before submitting.");
			}
			answers.put(question.id(), answer);
			boolean correct = isCorrect(question, answer);
			if (correct) {
				correctAnswers += 1;
			}
			items.add(new QuizSubmissionItem(
					question.number(),
					question.prompt(),
					answer,
					correctAnswerLabel(question),
					correct
			));
		}

		QuizAttempt attempt = new QuizAttempt();
		User student = userRepository.findByStudentId(studentId).orElse(null);
		attempt.setQuizId(quiz.getId());
		attempt.setStudentId(studentId);
		attempt.setStudentName(buildStudentName(student, studentId));
		attempt.setCourse(student != null ? normalize(student.getCourse()) : "");
		attempt.setSection(student != null ? normalize(student.getSectionName()) : "");
		attempt.setAttemptNumber(attemptNumber);
		attempt.setTotalQuestions(randomizedQuestions.size());
		attempt.setCorrectAnswers(correctAnswers);
		attempt.setAnswersJson(writeJson(answers));
		QuizAttempt savedAttempt = quizAttemptRepository.save(attempt);

		long updatedAttemptsUsed = attemptsUsed + 1;
		return new QuizSubmissionResult(
				savedAttempt.getId(),
				quiz.getId(),
				attemptNumber,
				randomizedQuestions.size(),
				correctAnswers,
				quiz.getAttempts(),
				Math.toIntExact(updatedAttemptsUsed),
				computeAttemptsRemaining(quiz.getAttempts(), updatedAttemptsUsed),
				items
		);
	}

	@Transactional(readOnly = true)
	public QuizSubmissionResult loadAttemptResult(Quiz quiz, String studentId, Integer attemptNumber) {
		if (attemptNumber == null || attemptNumber < 1) {
			throw new IllegalArgumentException("Attempt number is required.");
		}
		QuizAttemptRepository.AttemptResultProjection attempt = quizAttemptRepository.findAttemptResult(quiz.getId(), studentId, attemptNumber)
				.orElseThrow(() -> new java.util.NoSuchElementException("Quiz attempt not found."));

		List<QuestionDefinition> randomizedQuestions =
				randomizeQuestions(parseQuestions(quiz.getQuestionsJson()), quiz.getId(), studentId, attempt.getAttemptNumber());
		Map<String, String> answers = readAnswers(attempt.getAnswersJson());
		List<QuizSubmissionItem> items = new ArrayList<>();
		for (QuestionDefinition question : randomizedQuestions) {
			String answer = normalize(answers.get(question.id()));
			items.add(new QuizSubmissionItem(
					question.number(),
					question.prompt(),
					answer,
					correctAnswerLabel(question),
					isCorrect(question, answer)
			));
		}

		long attemptsUsed = countAttempts(quiz.getId(), studentId);
		return new QuizSubmissionResult(
				attempt.getId(),
				quiz.getId(),
				attempt.getAttemptNumber(),
				attempt.getTotalQuestions(),
				attempt.getCorrectAnswers(),
				quiz.getAttempts(),
				Math.toIntExact(attemptsUsed),
				computeAttemptsRemaining(quiz.getAttempts(), attemptsUsed),
				items
		);
	}

	private void ensureAttemptsAvailable(Quiz quiz, long attemptsUsed) {
		Integer limit = quiz.getAttempts();
		if (limit != null && limit > 0 && attemptsUsed >= limit) {
			throw new IllegalArgumentException("You have already used all allowed attempts for this quiz.");
		}
	}

	private Integer computeAttemptsRemaining(Integer limit, long attemptsUsed) {
		if (limit == null || limit <= 0) {
			return null;
		}
		return Math.max(limit - Math.toIntExact(attemptsUsed), 0);
	}

	private List<QuestionDefinition> parseQuestions(String raw) {
		if (raw == null || raw.isBlank()) {
			return List.of();
		}
		try {
			List<QuestionDefinition> parsed = objectMapper.readValue(raw, QUESTION_LIST_TYPE);
			return parsed == null ? List.of() : parsed;
		} catch (JsonProcessingException exception) {
			throw new IllegalArgumentException("Quiz questions are invalid.");
		}
	}

	private List<QuestionDefinition> randomizeQuestions(
			List<QuestionDefinition> source,
			Long quizId,
			String studentId,
			int attemptNumber
	) {
		List<QuestionDefinition> questions = new ArrayList<>();
		for (int index = 0; index < source.size(); index += 1) {
			QuestionDefinition question = source.get(index);
			if (question == null) {
				continue;
			}
			questions.add(question.withId("q" + (index + 1)));
		}

		Random questionRandom = new Random(Objects.hash(quizId, studentId, attemptNumber, "questions"));
		Collections.shuffle(questions, questionRandom);

		List<QuestionDefinition> randomized = new ArrayList<>();
		for (int index = 0; index < questions.size(); index += 1) {
			QuestionDefinition question = questions.get(index).withNumber(index + 1);
			if (!"multiple_choice".equals(question.type()) || question.choices() == null || question.choices().isEmpty()) {
				randomized.add(question);
				continue;
			}

			List<Map.Entry<String, String>> entries = new ArrayList<>(question.choices().entrySet());
			Random choiceRandom = new Random(Objects.hash(quizId, studentId, attemptNumber, question.id(), "choices"));
			Collections.shuffle(entries, choiceRandom);

			LinkedHashMap<String, String> shuffledChoices = new LinkedHashMap<>();
			String correctValue = question.choices().get(question.correct());
			String newCorrect = null;
			for (int choiceIndex = 0; choiceIndex < entries.size(); choiceIndex += 1) {
				String key = String.valueOf((char) ('A' + choiceIndex));
				String value = normalize(entries.get(choiceIndex).getValue());
				shuffledChoices.put(key, value);
				if (Objects.equals(value, normalize(correctValue))) {
					newCorrect = key;
				}
			}
			randomized.add(question.withChoices(shuffledChoices, newCorrect));
		}
		return randomized;
	}

	private List<StudentQuestion> toStudentQuestions(List<QuestionDefinition> questions) {
		List<StudentQuestion> items = new ArrayList<>();
		for (QuestionDefinition question : questions) {
			List<String> options = null;
			if ("multiple_choice".equals(question.type()) && question.choices() != null) {
				options = new ArrayList<>(question.choices().values());
			}
			items.add(new StudentQuestion(
					question.id(),
					question.number(),
					question.type(),
					question.prompt(),
					options
			));
		}
		return items;
	}

	private boolean isCorrect(QuestionDefinition question, String submittedAnswer) {
		String answer = normalize(submittedAnswer);
		if ("multiple_choice".equals(question.type())) {
			return answer.equals(correctAnswerLabel(question));
		}
		if ("identification".equals(question.type())) {
			String expected = correctAnswerLabel(question);
			if ("case_sensitive".equalsIgnoreCase(normalize(question.matchCase()))) {
				return answer.equals(expected);
			}
			return answer.equalsIgnoreCase(expected);
		}
		return false;
	}

	private String correctAnswerLabel(QuestionDefinition question) {
		if ("multiple_choice".equals(question.type()) && question.choices() != null) {
			return normalize(question.choices().get(question.correct()));
		}
		if ("identification".equals(question.type())) {
			return normalize(question.answer());
		}
		return "";
	}

	private String writeJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Unable to store quiz attempt.", exception);
		}
	}

	private Map<String, String> readAnswers(String raw) {
		if (raw == null || raw.isBlank()) {
			return Map.of();
		}
		try {
			Map<String, String> parsed = objectMapper.readValue(raw, new TypeReference<Map<String, String>>() {
			});
			return parsed == null ? Map.of() : parsed;
		} catch (JsonProcessingException exception) {
			throw new IllegalArgumentException("Quiz attempt data is invalid.");
		}
	}

	private static String normalize(String value) {
		return value == null ? "" : value.trim();
	}

	private static String buildStudentName(User student, String fallbackStudentId) {
		if (student == null) {
			return normalize(fallbackStudentId);
		}
		String lastName = normalize(student.getLastName());
		String firstName = normalize(student.getFirstName());
		String middleInitial = normalize(student.getMiddleInitial());
		StringBuilder builder = new StringBuilder();
		if (!lastName.isBlank()) {
			builder.append(lastName);
		}
		if (!firstName.isBlank()) {
			if (!builder.isEmpty()) {
				builder.append(", ");
			}
			builder.append(firstName);
		}
		if (!middleInitial.isBlank()) {
			builder.append(" ").append(middleInitial);
		}
		if (builder.isEmpty()) {
			return normalize(fallbackStudentId);
		}
		return builder.toString();
	}

	public record StudentQuizSession(
			Long quizId,
			String title,
			String subject,
			String dueDate,
			String description,
			Integer durationMinutes,
			Integer attemptsAllowed,
			Integer attemptsUsed,
			Integer attemptsRemaining,
			List<StudentQuestion> questions
	) {
	}

	public record StudentQuestion(
			String id,
			Integer number,
			String type,
			String prompt,
			List<String> options
	) {
	}

	public record QuizSubmissionResult(
			Long attemptId,
			Long quizId,
			Integer attemptNumber,
			Integer totalQuestions,
			Integer correctAnswers,
			Integer attemptsAllowed,
			Integer attemptsUsed,
			Integer attemptsRemaining,
			List<QuizSubmissionItem> items
	) {
	}

	public record QuizSubmissionItem(
			Integer number,
			String prompt,
			String answer,
			String correctAnswer,
			boolean correct
	) {
	}

	public record LatestQuizAttemptSummary(
			Integer attemptNumber,
			Integer correctAnswers,
			Integer totalQuestions
	) {
	}

	@SuppressWarnings("unused")
	private record QuestionDefinition(
			String id,
			Integer number,
			String type,
			String question,
			Map<String, String> choices,
			String correct,
			String answer,
			String matchCase
	) {
		String prompt() {
			return question == null ? "" : question;
		}

		QuestionDefinition withId(String nextId) {
			return new QuestionDefinition(nextId, number, normalizeType(type), question, normalizedChoices(choices), correct, answer, matchCase);
		}

		QuestionDefinition withNumber(int nextNumber) {
			return new QuestionDefinition(id, nextNumber, normalizeType(type), question, normalizedChoices(choices), correct, answer, matchCase);
		}

		QuestionDefinition withChoices(Map<String, String> nextChoices, String nextCorrect) {
			return new QuestionDefinition(id, number, normalizeType(type), question, normalizedChoices(nextChoices), nextCorrect, answer, matchCase);
		}

		private static String normalizeType(String raw) {
			return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
		}

		private static Map<String, String> normalizedChoices(Map<String, String> raw) {
			if (raw == null || raw.isEmpty()) {
				return Map.of();
			}
			LinkedHashMap<String, String> normalized = new LinkedHashMap<>();
			for (Map.Entry<String, String> entry : raw.entrySet()) {
				normalized.put(entry.getKey(), normalize(entry.getValue()));
			}
			return normalized;
		}
	}
}
