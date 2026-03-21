package com.example.Gnosis.quiz;

import com.example.Gnosis.user.User;
import com.example.Gnosis.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
public class ProfessorQuizResultsService {
	private final QuizRepository quizRepository;
	private final QuizAttemptRepository quizAttemptRepository;
	private final UserRepository userRepository;

	public ProfessorQuizResultsService(
			QuizRepository quizRepository,
			QuizAttemptRepository quizAttemptRepository,
			UserRepository userRepository
	) {
		this.quizRepository = quizRepository;
		this.quizAttemptRepository = quizAttemptRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public List<QuizResultCard> listQuizCards(String professorId) {
		return quizRepository.findByProfessorIdOrderByCreatedAtDesc(professorId)
				.stream()
				.map(quiz -> {
					long totalAttempts = quizAttemptRepository.countByQuizId(quiz.getId());
					long studentCount = quizAttemptRepository.findByQuizIdOrderByStudentIdAscAttemptsCountDesc(quiz.getId())
							.stream()
							.map(QuizAttempt::getStudentId)
							.filter(this::hasText)
							.map(String::trim)
							.distinct()
							.count();
					return new QuizResultCard(
							quiz.getId(),
							quiz.getTitle(),
							quiz.getDescription(),
							quiz.getSubject(),
							quiz.getSection(),
							resolveDisplayStatus(quiz),
							Math.toIntExact(totalAttempts),
							Math.toIntExact(studentCount)
					);
				})
				.toList();
	}

	@Transactional(readOnly = true)
	public QuizResultDetails getQuizResults(Long quizId, String professorId) {
		Quiz quiz = quizRepository.findById(quizId)
				.orElseThrow(() -> new NoSuchElementException("Quiz not found."));
		if (!hasText(professorId) || !professorId.trim().equals(quiz.getProfessorId())) {
			throw new IllegalArgumentException("Unauthorized quiz access.");
		}

		List<QuizAttempt> attempts = quizAttemptRepository.findByQuizIdOrderByStudentIdAscAttemptsCountDesc(quizId);
		if (attempts.isEmpty()) {
			return new QuizResultDetails(
					quiz.getId(),
					quiz.getTitle(),
					quiz.getDescription(),
					quiz.getSubject(),
					quiz.getSection(),
					List.of()
			);
		}

		LinkedHashMap<String, StudentAttemptAccumulator> grouped = new LinkedHashMap<>();
		for (QuizAttempt attempt : attempts) {
			String studentId = normalize(attempt.getStudentId());
			if (studentId == null) {
				continue;
			}
			StudentAttemptAccumulator accumulator = grouped.computeIfAbsent(studentId, StudentAttemptAccumulator::new);
			accumulator.record(attempt);
		}

		Map<String, User> usersByStudentId = loadUsers(grouped.keySet());
		List<StudentQuizResultRow> rows = new ArrayList<>();
		for (StudentAttemptAccumulator accumulator : grouped.values()) {
			User user = usersByStudentId.get(accumulator.studentId);
			String studentName = fallback(accumulator.studentName, user != null ? buildStudentName(user) : accumulator.studentId);
			String course = fallback(accumulator.course, user != null ? user.getCourse() : "-");
			String section = fallback(accumulator.section, user != null ? user.getSectionName() : "-");
			Integer bestCorrectAnswers = accumulator.bestCorrectAnswers;
			Integer bestTotalQuestions = accumulator.bestTotalQuestions;
			double scorePercent = computeScorePercent(bestCorrectAnswers, bestTotalQuestions);
			rows.add(new StudentQuizResultRow(
					accumulator.studentId,
					studentName,
					course,
					section,
					accumulator.attemptsTaken,
					bestCorrectAnswers,
					bestTotalQuestions,
					scorePercent,
					false,
					false
			));
		}

		double highestScore = rows.stream()
				.map(StudentQuizResultRow::scorePercent)
				.max(Double::compareTo)
				.orElse(0.0d);

		List<StudentQuizResultRow> flaggedRows = rows.stream()
				.map(row -> new StudentQuizResultRow(
						row.studentId(),
						row.studentName(),
						row.course(),
						row.section(),
						row.attemptsTaken(),
						row.correctAnswers(),
						row.totalQuestions(),
						row.scorePercent(),
						Double.compare(row.scorePercent(), highestScore) == 0,
						row.scorePercent() > 0.0d && row.scorePercent() < 50.0d
				))
				.sorted(Comparator.comparing(StudentQuizResultRow::studentName, String.CASE_INSENSITIVE_ORDER))
				.toList();

		return new QuizResultDetails(
				quiz.getId(),
				quiz.getTitle(),
				quiz.getDescription(),
				quiz.getSubject(),
				quiz.getSection(),
				flaggedRows
		);
	}

	private Map<String, User> loadUsers(Set<String> studentIds) {
		if (studentIds == null || studentIds.isEmpty()) {
			return Map.of();
		}
		Map<String, User> users = new LinkedHashMap<>();
		for (User user : userRepository.findByStudentIdIn(studentIds)) {
			String studentId = normalize(user.getStudentId());
			if (studentId != null) {
				users.put(studentId, user);
			}
		}
		return users;
	}

	private static String buildStudentName(User user) {
		String lastName = normalize(user.getLastName());
		String firstName = normalize(user.getFirstName());
		String middleInitial = normalize(user.getMiddleInitial());
		StringBuilder builder = new StringBuilder();
		if (lastName != null) {
			builder.append(lastName);
		}
		if (firstName != null) {
			if (!builder.isEmpty()) {
				builder.append(", ");
			}
			builder.append(firstName);
		}
		if (middleInitial != null) {
			builder.append(" ").append(middleInitial);
		}
		return builder.isEmpty() ? "-" : builder.toString();
	}

	private static double computeScorePercent(Integer correctAnswers, Integer totalQuestions) {
		if (correctAnswers == null || totalQuestions == null || totalQuestions < 1) {
			return 0.0d;
		}
		return (correctAnswers * 100.0d) / totalQuestions;
	}

	private static String normalize(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private boolean hasText(String value) {
		return normalize(value) != null;
	}

	private static String fallback(String value, String defaultValue) {
		String normalized = normalize(value);
		return normalized != null ? normalized : defaultValue;
	}

	private static String resolveDisplayStatus(Quiz quiz) {
		if (quiz == null) {
			return null;
		}
		String status = normalize(quiz.getStatus());
		if (status == null) {
			return QuizService.isExpired(quiz) ? "EXPIRED" : null;
		}
		if (!"archived".equalsIgnoreCase(status) && QuizService.isExpired(quiz)) {
			return "EXPIRED";
		}
		return status.toUpperCase();
	}

	public record QuizResultCard(
			Long id,
			String title,
			String description,
			String subject,
			String section,
			String status,
			Integer totalAttempts,
			Integer studentCount
	) {
	}

	public record QuizResultDetails(
			Long quizId,
			String title,
			String description,
			String subject,
			String section,
			List<StudentQuizResultRow> rows
	) {
	}

	public record StudentQuizResultRow(
			String studentId,
			String studentName,
			String course,
			String section,
			Integer attemptsTaken,
			Integer correctAnswers,
			Integer totalQuestions,
			double scorePercent,
			boolean highestScorer,
			boolean lowScore
	) {
	}

	private static final class StudentAttemptAccumulator {
		private final String studentId;
		private int attemptsTaken;
		private Integer bestCorrectAnswers;
		private Integer bestTotalQuestions;
		private double bestScorePercent = Double.NEGATIVE_INFINITY;
		private String studentName;
		private String course;
		private String section;

		private StudentAttemptAccumulator(String studentId) {
			this.studentId = studentId;
		}

		private void record(QuizAttempt attempt) {
			attemptsTaken += 1;
			if (studentName == null) {
				studentName = normalize(attempt.getStudentName());
			}
			if (course == null) {
				course = normalize(attempt.getCourse());
			}
			if (section == null) {
				section = normalize(attempt.getSection());
			}
			Integer correctAnswers = attempt.getCorrectAnswers();
			Integer totalQuestions = attempt.getTotalQuestions();
			double scorePercent = computeScorePercent(correctAnswers, totalQuestions);
			if (scorePercent > bestScorePercent) {
				bestScorePercent = scorePercent;
				bestCorrectAnswers = correctAnswers;
				bestTotalQuestions = totalQuestions;
				return;
			}
			if (Double.compare(scorePercent, bestScorePercent) == 0
					&& (correctAnswers != null && bestCorrectAnswers != null)
					&& correctAnswers > bestCorrectAnswers) {
				bestCorrectAnswers = correctAnswers;
				bestTotalQuestions = totalQuestions;
			}
		}
	}
}
