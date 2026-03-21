package com.example.Gnosis.web;

import com.example.Gnosis.announcement.AnnouncementResponse;
import com.example.Gnosis.announcement.AnnouncementService;
import com.example.Gnosis.assignment.AssignmentResponse;
import com.example.Gnosis.assignment.AssignmentService;
import com.example.Gnosis.lesson.LessonResponse;
import com.example.Gnosis.lesson.LessonService;
import com.example.Gnosis.quiz.QuizResponse;
import com.example.Gnosis.quiz.QuizService;
import com.example.Gnosis.schoolclass.SchoolClassDto;
import com.example.Gnosis.schoolclass.SchoolClassService;
import com.example.Gnosis.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class StudentNotificationService {
	private static final int MAX_ITEMS = 12;

	private final AnnouncementService announcementService;
	private final AssignmentService assignmentService;
	private final LessonService lessonService;
	private final QuizService quizService;
	private final UserRepository userRepository;
	private final SchoolClassService schoolClassService;

	public StudentNotificationService(
			AnnouncementService announcementService,
			AssignmentService assignmentService,
			LessonService lessonService,
			QuizService quizService,
			UserRepository userRepository,
			SchoolClassService schoolClassService
	) {
		this.announcementService = announcementService;
		this.assignmentService = assignmentService;
		this.lessonService = lessonService;
		this.quizService = quizService;
		this.userRepository = userRepository;
		this.schoolClassService = schoolClassService;
	}

	@Transactional(readOnly = true)
	public NotificationPayload list(Authentication authentication) {
		StudentContext context = resolveStudentContext(authentication);
		Set<String> allowedSubjects = resolveAllowedSubjects(context);

		List<NotificationItem> items = java.util.stream.Stream.of(
						announcementService.listForStudentSection(context.section()).stream()
								.map(this::toAnnouncementNotification),
						assignmentService.listForStudentSection(context.section(), allowedSubjects).stream()
								.map(this::toAssignmentNotification),
						lessonService.listForStudentSection(context.section(), allowedSubjects).stream()
								.map(this::toLessonNotification),
						quizService.listForStudentSection(context.section(), allowedSubjects).stream()
								.map(this::toQuizNotification)
				)
				.flatMap(stream -> stream)
				.filter(item -> item.createdAt() != null)
				.sorted(Comparator.comparing(NotificationItem::createdAt).reversed())
				.limit(MAX_ITEMS)
				.toList();

		return new NotificationPayload(items);
	}

	private NotificationItem toAnnouncementNotification(AnnouncementResponse response) {
		return new NotificationItem(
				"announcement-" + response.getId(),
				"announcement",
				defaultText(response.getTitle(), "New announcement"),
				defaultText(response.getSubject(), "Announcement"),
				defaultText(response.getProfessorName(), "Instructor") + " posted a new announcement.",
				"/student/dashboard",
				response.getCreatedAt()
		);
	}

	private NotificationItem toAssignmentNotification(AssignmentResponse response) {
		return new NotificationItem(
				"assignment-" + response.getId(),
				"assignment",
				defaultText(response.getTitle(), "New assignment"),
				defaultText(response.getSubject(), "Assignment"),
				"New assignment posted" + formatSuffix(response.getDueDate(), "Due "),
				"/student/assignments",
				response.getCreatedAt()
		);
	}

	private NotificationItem toLessonNotification(LessonResponse response) {
		return new NotificationItem(
				"lesson-" + response.getId(),
				"lesson",
				defaultText(response.getTitle(), "New lesson"),
				defaultText(response.getSubject(), "Lesson"),
				"New lesson material uploaded for " + defaultText(response.getWeek(), "this week") + ".",
				"/student/lessons",
				response.getCreatedAt()
		);
	}

	private NotificationItem toQuizNotification(QuizResponse response) {
		return new NotificationItem(
				"quiz-" + response.getId(),
				"quiz",
				defaultText(response.getTitle(), "New quiz"),
				defaultText(response.getSubject(), "Quiz"),
				"New quiz is available" + formatSuffix(response.getDueDate(), "Due "),
				"/student/quiz",
				response.getCreatedAt()
		);
	}

	private StudentContext resolveStudentContext(Authentication authentication) {
		if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
			return new StudentContext(null, null);
		}
		return userRepository.findByStudentId(authentication.getName().trim())
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

	private static String defaultText(String value, String defaultValue) {
		String normalized = normalizeValue(value);
		return normalized != null ? normalized : defaultValue;
	}

	private static String formatSuffix(String value, String prefix) {
		String normalized = normalizeValue(value);
		return normalized != null ? ". " + prefix + normalized : ".";
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

	private record StudentContext(String course, String section) {}

	public record NotificationPayload(List<NotificationItem> items) {}

	public record NotificationItem(
			String id,
			String type,
			String title,
			String subject,
			String message,
			String link,
			Instant createdAt
	) {}
}
