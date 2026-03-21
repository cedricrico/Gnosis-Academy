package com.example.Gnosis.grade;

import com.example.Gnosis.assignment.AssignmentSubmission;
import com.example.Gnosis.assignment.AssignmentSubmissionRepository;
import com.example.Gnosis.quiz.Quiz;
import com.example.Gnosis.quiz.QuizAttempt;
import com.example.Gnosis.quiz.QuizAttemptRepository;
import com.example.Gnosis.quiz.QuizRepository;
import com.example.Gnosis.schoolclass.SchoolClassDto;
import com.example.Gnosis.schoolclass.SchoolClassService;
import com.example.Gnosis.user.User;
import com.example.Gnosis.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ProfessorGradeRecordService {
	private static final double ASSIGNMENT_WEIGHT = 30.0;
	private static final double QUIZ_WEIGHT = 20.0;

	private final SchoolClassService schoolClassService;
	private final UserRepository userRepository;
	private final AssignmentSubmissionRepository assignmentSubmissionRepository;
	private final QuizRepository quizRepository;
	private final QuizAttemptRepository quizAttemptRepository;

	public ProfessorGradeRecordService(
			SchoolClassService schoolClassService,
			UserRepository userRepository,
			AssignmentSubmissionRepository assignmentSubmissionRepository,
			QuizRepository quizRepository,
			QuizAttemptRepository quizAttemptRepository
	) {
		this.schoolClassService = schoolClassService;
		this.userRepository = userRepository;
		this.assignmentSubmissionRepository = assignmentSubmissionRepository;
		this.quizRepository = quizRepository;
		this.quizAttemptRepository = quizAttemptRepository;
	}

	@Transactional(readOnly = true)
	public GradeRecordResponse listGradeRecords(String professorId, String professorName) {
		Map<String, StudentAccumulator> students = new LinkedHashMap<>();

		for (SchoolClassDto schoolClass : schoolClassService.findForProfessor(professorId, professorName)) {
			String courseName = safeTrim(schoolClass.getCourseName());
			String courseCode = safeTrim(schoolClass.getCourseCode());
			String sectionName = safeTrim(schoolClass.getSectionName());
			if (courseName == null || sectionName == null) {
				continue;
			}
			for (User student : findStudentsForSection(courseName, courseCode, sectionName)) {
				StudentAccumulator accumulator = students.computeIfAbsent(
						safeTrim(student.getStudentId()),
						ignored -> new StudentAccumulator(
								safeTrim(student.getStudentId()),
								buildStudentName(student),
								safeTrim(student.getCourse()),
								safeTrim(student.getSectionName())
						)
				);
				accumulator.fillMissing(buildStudentName(student), safeTrim(student.getCourse()), safeTrim(student.getSectionName()));
			}
		}

		List<AssignmentSubmission> gradedSubmissions =
				assignmentSubmissionRepository.findByProfessorIdAndGradeIsNotNullOrderByStudentNameAscStudentIdAsc(professorId);
		for (AssignmentSubmission submission : gradedSubmissions) {
			String studentId = safeTrim(submission.getStudentId());
			if (studentId == null) {
				continue;
			}
			StudentAccumulator accumulator = students.computeIfAbsent(
					studentId,
					ignored -> new StudentAccumulator(
							studentId,
							safeTrim(submission.getStudentName()),
							safeTrim(submission.getStudentCourse()),
							safeTrim(submission.getStudentSection())
					)
			);
			accumulator.fillMissing(
					safeTrim(submission.getStudentName()),
					safeTrim(submission.getStudentCourse()),
					safeTrim(submission.getStudentSection())
			);
			Integer grade = submission.getGrade();
			Integer points = submission.getAssignmentPoints();
			if (grade != null && points != null && points > 0) {
				accumulator.assignmentEarned += grade;
				accumulator.assignmentPossible += points;
				accumulator.assignmentCount += 1;
			}
		}

		List<Long> quizIds = quizRepository.findByProfessorIdOrderByCreatedAtDesc(professorId).stream()
				.map(Quiz::getId)
				.toList();
		if (!quizIds.isEmpty()) {
			Map<String, QuizBestAttempt> bestAttempts = new LinkedHashMap<>();
			for (QuizAttempt attempt : quizAttemptRepository.findByQuizIdInOrderByStudentIdAscQuizIdAscAttemptsCountDesc(quizIds)) {
				String studentId = safeTrim(attempt.getStudentId());
				Long quizId = attempt.getQuizId();
				if (studentId == null || quizId == null) {
					continue;
				}
				String key = quizId + "::" + studentId;
				QuizBestAttempt candidate = new QuizBestAttempt(
						studentId,
						safeTrim(attempt.getStudentName()),
						safeTrim(attempt.getCourse()),
						safeTrim(attempt.getSection()),
						attempt.getCorrectAnswers(),
						attempt.getTotalQuestions()
				);
				QuizBestAttempt current = bestAttempts.get(key);
				if (current == null || candidate.isBetterThan(current)) {
					bestAttempts.put(key, candidate);
				}
			}

			for (QuizBestAttempt bestAttempt : bestAttempts.values()) {
				StudentAccumulator accumulator = students.computeIfAbsent(
						bestAttempt.studentId,
						ignored -> new StudentAccumulator(
								bestAttempt.studentId,
								bestAttempt.studentName,
								bestAttempt.course,
								bestAttempt.section
						)
				);
				accumulator.fillMissing(bestAttempt.studentName, bestAttempt.course, bestAttempt.section);
				if (bestAttempt.correctAnswers != null && bestAttempt.totalQuestions != null && bestAttempt.totalQuestions > 0) {
					accumulator.quizEarned += bestAttempt.correctAnswers;
					accumulator.quizPossible += bestAttempt.totalQuestions;
					accumulator.quizCount += 1;
				}
			}
		}

		List<GradeRecordRow> rows = students.values().stream()
				.map(StudentAccumulator::toRow)
				.sorted(Comparator
						.comparing(GradeRecordRow::studentName, String.CASE_INSENSITIVE_ORDER)
						.thenComparing(GradeRecordRow::studentId, String.CASE_INSENSITIVE_ORDER))
				.toList();
		return new GradeRecordResponse(rows, summarize(rows));
	}

	private List<User> findStudentsForSection(String courseName, String courseCode, String sectionName) {
		List<User> combined = new ArrayList<>();
		Set<String> seen = new HashSet<>();

		String combinedCourseName = courseName + " " + sectionName;
		String combinedCourseCode = courseCode != null ? courseCode + " " + sectionName : null;

		List<String> courseVariants = new ArrayList<>();
		courseVariants.add(courseName);
		if (courseCode != null && !courseCode.equalsIgnoreCase(courseName)) {
			courseVariants.add(courseCode);
		}
		courseVariants.add(combinedCourseName);
		if (combinedCourseCode != null && !combinedCourseCode.equalsIgnoreCase(combinedCourseName)) {
			courseVariants.add(combinedCourseCode);
		}

		List<String> sectionVariants = buildSectionVariants(sectionName, courseName, courseCode);

		for (String courseVariant : courseVariants) {
			if (courseVariant == null || courseVariant.isBlank()) {
				continue;
			}
			for (String sectionVariant : sectionVariants) {
				List<User> students = userRepository
						.findByCourseIgnoreCaseAndSectionNameIgnoreCaseOrderByLastNameAscFirstNameAsc(courseVariant, sectionVariant);
				for (User student : students) {
					String id = safeTrim(student.getStudentId());
					if (id == null || !seen.add(id)) {
						continue;
					}
					combined.add(student);
				}
			}
		}

		for (String sectionVariant : sectionVariants) {
			List<User> bySectionOnly = userRepository
					.findBySectionNameIgnoreCaseOrderByLastNameAscFirstNameAsc(sectionVariant);
			for (User student : bySectionOnly) {
				String id = safeTrim(student.getStudentId());
				if (id == null || !seen.add(id)) {
					continue;
				}
				combined.add(student);
			}
		}

		String normalizedCourseName = normalizeKey(courseName);
		String normalizedCourseCode = courseCode != null ? normalizeKey(courseCode) : "";
		List<User> byCourseContainingSection = userRepository
				.findByCourseIgnoreCaseContainingOrderByLastNameAscFirstNameAsc(sectionName);
		for (User student : byCourseContainingSection) {
			String id = safeTrim(student.getStudentId());
			if (id == null || !seen.add(id)) {
				continue;
			}
			String studentCourse = normalizeKey(student.getCourse());
			boolean sectionMatch = sectionVariants.stream()
					.map(ProfessorGradeRecordService::normalizeKey)
					.anyMatch(normalizedSection -> !normalizedSection.isEmpty() && studentCourse.contains(normalizedSection));
			boolean courseMatch = normalizedCourseName.isEmpty() && normalizedCourseCode.isEmpty()
					|| (!normalizedCourseName.isEmpty() && studentCourse.contains(normalizedCourseName))
					|| (!normalizedCourseCode.isEmpty() && studentCourse.contains(normalizedCourseCode));
			if (sectionMatch && courseMatch) {
				combined.add(student);
			}
		}

		return combined;
	}

	private static List<String> buildSectionVariants(String sectionName, String courseName, String courseCode) {
		LinkedHashSet<String> variants = new LinkedHashSet<>();
		String base = safeTrim(sectionName);
		if (base != null) {
			variants.add(base);
		}
		String courseCodeTrimmed = safeTrim(courseCode);
		String courseNameTrimmed = safeTrim(courseName);
		if (base != null && courseCodeTrimmed != null) {
			variants.add(courseCodeTrimmed + " " + base);
		}
		if (base != null && courseNameTrimmed != null) {
			variants.add(courseNameTrimmed + " " + base);
		}
		String stripped = stripCoursePrefix(base, courseCode);
		if (stripped != null) {
			variants.add(stripped);
		}
		String strippedName = stripCoursePrefix(base, courseName);
		if (strippedName != null) {
			variants.add(strippedName);
		}
		return new ArrayList<>(variants);
	}

	private static String stripCoursePrefix(String sectionName, String coursePrefix) {
		if (sectionName == null || coursePrefix == null) {
			return null;
		}
		String sectionTrimmed = sectionName.trim();
		String prefixTrimmed = coursePrefix.trim();
		if (sectionTrimmed.isEmpty() || prefixTrimmed.isEmpty()) {
			return null;
		}
		if (!sectionTrimmed.regionMatches(true, 0, prefixTrimmed, 0, prefixTrimmed.length())) {
			return null;
		}
		String remainder = sectionTrimmed.substring(prefixTrimmed.length());
		remainder = remainder.replaceFirst("^[\\s\\-_/]+", "");
		return safeTrim(remainder);
	}

	private static String buildStudentName(User student) {
		String last = safeTrim(student.getLastName());
		String first = safeTrim(student.getFirstName());
		String middle = safeTrim(student.getMiddleInitial());
		StringBuilder builder = new StringBuilder();
		if (last != null) {
			builder.append(last);
		}
		if (first != null) {
			if (!builder.isEmpty()) {
				builder.append(", ");
			}
			builder.append(first);
		}
		if (middle != null) {
			builder.append(" ").append(middle);
		}
		return builder.isEmpty() ? "-" : builder.toString();
	}

	private static String safeTrim(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static String normalizeKey(String value) {
		if (value == null) {
			return "";
		}
		String normalized = value.trim()
				.replace('\\', ' ')
				.replace('/', ' ')
				.replace('-', ' ')
				.replace('_', ' ')
				.replaceAll("\\s+", " ");
		return normalized.toLowerCase(Locale.ROOT);
	}

	public record GradeRecordRow(
			String studentId,
			String studentName,
			String course,
			String section,
			Double assignmentPercent,
			Integer assignmentCount,
			Double quizPercent,
			Integer quizCount,
			Double totalPercent,
			String letterGrade
	) {
	}

	public record GradeRecordSummary(
			Double classAverage,
			Double passRate,
			Double highestScore,
			Double lowestScore,
			Integer gradedStudentCount
	) {
	}

	public record GradeRecordResponse(
			List<GradeRecordRow> rows,
			GradeRecordSummary summary
	) {
	}

	private static final class StudentAccumulator {
		private final String studentId;
		private String studentName;
		private String course;
		private String section;
		private double assignmentEarned;
		private double assignmentPossible;
		private int assignmentCount;
		private double quizEarned;
		private double quizPossible;
		private int quizCount;

		private StudentAccumulator(String studentId, String studentName, String course, String section) {
			this.studentId = studentId;
			this.studentName = studentName;
			this.course = course;
			this.section = section;
		}

		private void fillMissing(String studentName, String course, String section) {
			if (this.studentName == null) {
				this.studentName = studentName;
			}
			if (this.course == null) {
				this.course = course;
			}
			if (this.section == null) {
				this.section = section;
			}
		}

		private GradeRecordRow toRow() {
			Double assignmentPercent = assignmentPossible > 0 ? roundPercent((assignmentEarned / assignmentPossible) * 100.0) : null;
			Double quizPercent = quizPossible > 0 ? roundPercent((quizEarned / quizPossible) * 100.0) : null;
			Double totalPercent = computeCurrentTotal(assignmentPercent, quizPercent);
			return new GradeRecordRow(
					studentId != null ? studentId : "-",
					studentName != null ? studentName : "-",
					course,
					section,
					assignmentPercent,
					assignmentCount,
					quizPercent,
					quizCount,
					totalPercent,
					computeLetterGrade(totalPercent)
			);
		}
	}

	private record QuizBestAttempt(
			String studentId,
			String studentName,
			String course,
			String section,
			Integer correctAnswers,
			Integer totalQuestions
	) {
		private boolean isBetterThan(QuizBestAttempt other) {
			double thisPercent = computeScorePercent(correctAnswers, totalQuestions);
			double otherPercent = computeScorePercent(other.correctAnswers, other.totalQuestions);
			if (Double.compare(thisPercent, otherPercent) != 0) {
				return thisPercent > otherPercent;
			}
			int thisCorrect = correctAnswers != null ? correctAnswers : -1;
			int otherCorrect = other.correctAnswers != null ? other.correctAnswers : -1;
			return thisCorrect > otherCorrect;
		}
	}

	private static Double computeCurrentTotal(Double assignmentPercent, Double quizPercent) {
		double weighted = 0.0;
		double activeWeight = 0.0;
		if (assignmentPercent != null) {
			weighted += assignmentPercent * ASSIGNMENT_WEIGHT;
			activeWeight += ASSIGNMENT_WEIGHT;
		}
		if (quizPercent != null) {
			weighted += quizPercent * QUIZ_WEIGHT;
			activeWeight += QUIZ_WEIGHT;
		}
		if (activeWeight <= 0.0) {
			return null;
		}
		return roundPercent(weighted / activeWeight);
	}

	private static GradeRecordSummary summarize(List<GradeRecordRow> rows) {
		List<Double> totals = rows.stream()
				.map(GradeRecordRow::totalPercent)
				.filter(java.util.Objects::nonNull)
				.toList();
		if (totals.isEmpty()) {
			return new GradeRecordSummary(null, null, null, null, 0);
		}
		double average = totals.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
		double highest = totals.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
		double lowest = totals.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
		long passed = totals.stream().filter(total -> total >= 60.0).count();
		double passRate = ((double) passed / (double) totals.size()) * 100.0;
		return new GradeRecordSummary(
				roundPercent(average),
				roundPercent(passRate),
				roundPercent(highest),
				roundPercent(lowest),
				totals.size()
		);
	}

	private static String computeLetterGrade(Double totalPercent) {
		if (totalPercent == null) {
			return "-";
		}
		if (totalPercent >= 90.0) {
			return "A";
		}
		if (totalPercent >= 80.0) {
			return "B";
		}
		if (totalPercent >= 70.0) {
			return "C";
		}
		if (totalPercent >= 60.0) {
			return "D";
		}
		return "F";
	}

	private static double computeScorePercent(Integer score, Integer total) {
		if (score == null || total == null || total <= 0) {
			return Double.NEGATIVE_INFINITY;
		}
		return ((double) score / (double) total) * 100.0;
	}

	private static double roundPercent(double value) {
		return Math.round(value * 100.0) / 100.0;
	}
}
