package com.example.Gnosis.web;

import com.example.Gnosis.user.User;
import com.example.Gnosis.user.UserRepository;
import com.example.Gnosis.announcement.AnnouncementResponse;
import com.example.Gnosis.announcement.AnnouncementService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/student")
public class StudentController {
	private final UserRepository userRepository;
	private final AnnouncementService announcementService;

	public StudentController(UserRepository userRepository, AnnouncementService announcementService) {
		this.userRepository = userRepository;
		this.announcementService = announcementService;
	}

	@ModelAttribute
	public void addStudentTemplateModel(Authentication authentication, Model model) {
		if (authentication == null) {
			return;
		}
		String studentId = authentication.getName();
		if (studentId == null || studentId.isBlank()) {
			return;
		}

		// Sensible defaults even if the DB record is missing/misconfigured.
		model.addAttribute("studentId", studentId);
		model.addAttribute("studentFullName", studentId);
		model.addAttribute("studentCourse", "Unassigned");
		model.addAttribute("studentSection", "Unassigned");
		model.addAttribute("studentStatus", "");

		userRepository.findByStudentId(studentId).ifPresent(user -> {
			model.addAttribute("studentId", user.getStudentId());
			model.addAttribute("studentFullName", formatStudentName(user));
			model.addAttribute("studentCourse", blankToDefault(user.getCourse(), "Unassigned"));
			model.addAttribute("studentSection", blankToDefault(user.getSectionName(), "Unassigned"));
			model.addAttribute("studentStatus", blankToDefault(user.getStatus(), ""));
		});
	}

	@GetMapping({"", "/"})
	public String studentRoot() {
		return "redirect:/student/dashboard";
	}

	@GetMapping("/dashboard")
	public String studentDashboard(Authentication authentication, Model model) {
		String studentId = authentication != null ? authentication.getName() : null;
		if (studentId != null && !studentId.isBlank()) {
			userRepository.findByStudentId(studentId).ifPresent(student -> {
				String sectionName = student.getSectionName();
				List<AnnouncementResponse> announcements = announcementService.listForStudentSection(sectionName);
				List<StudentAnnouncementRow> rows = announcements.stream()
						.map(StudentController::toStudentAnnouncementRow)
						.toList();
				model.addAttribute("studentAnnouncements", rows);
			});
		}
		if (!model.containsAttribute("studentAnnouncements")) {
			model.addAttribute("studentAnnouncements", List.of());
		}
		return "student/student-dashboard";
	}

	@GetMapping("/assignments")
	public String studentAssignments() {
		return "student/student-assignments";
	}

	@GetMapping("/lessons")
	public String studentLessons() {
		return "student/student-lessons";
	}

	@GetMapping("/profile")
	public String studentProfile() {
		return "student/student-profile";
	}

	@GetMapping("/records")
	public String studentRecords() {
		return "student/student-records";
	}

	@GetMapping("/quiz")
	public String studentQuiz() {
		return "student/student-quiz";
	}

	@GetMapping("/quiz/start")
	public String studentQuizStart() {
		return "student/student-quiz-start";
	}

	@GetMapping("/quiz/results")
	public String studentQuizResults() {
		return "student/student-quiz-results";
	}

	private static String blankToDefault(String value, String defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? defaultValue : trimmed;
	}

	private static String formatStudentName(User user) {
		String first = user.getFirstName() != null ? user.getFirstName().trim() : "";
		String last = user.getLastName() != null ? user.getLastName().trim() : "";
		String mi = user.getMiddleInitial() != null ? user.getMiddleInitial().trim() : "";

		StringBuilder sb = new StringBuilder();
		if (!first.isEmpty()) {
			sb.append(first);
		}
		if (!mi.isEmpty()) {
			if (sb.length() > 0) sb.append(' ');
			sb.append(mi).append('.');
		}
		if (!last.isEmpty()) {
			if (sb.length() > 0) sb.append(' ');
			sb.append(last);
		}
		String full = sb.toString().trim();
		return full.isEmpty() ? user.getStudentId() : full;
	}

	private static StudentAnnouncementRow toStudentAnnouncementRow(AnnouncementResponse response) {
		String title = blankToDefault(response.getTitle(), "Announcement");
		String content = blankToDefault(response.getContent(), "");
		String professor = blankToDefault(response.getProfessorName(), "Instructor");
		String subject = blankToDefault(response.getSubject(), "All Sections");
		String dateLabel = formatAnnouncementDate(response);
		String imageUrl = response.getImageName() != null && !response.getImageName().isBlank()
				? "/student/api/announcements/" + response.getId() + "/image"
				: null;
		return new StudentAnnouncementRow(title, content, dateLabel, professor, subject, imageUrl);
	}

	private static String formatAnnouncementDate(AnnouncementResponse response) {
		String postedOn = response.getPostedOn();
		if (postedOn != null && !postedOn.isBlank()) {
			String parsed = tryFormatDate(postedOn.trim());
			if (parsed != null) {
				return parsed;
			}
			return postedOn.trim();
		}
		Instant createdAt = response.getCreatedAt();
		if (createdAt == null) {
			return "";
		}
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);
		return formatter.format(createdAt.atZone(ZoneId.systemDefault()).toLocalDate());
	}

	private static String tryFormatDate(String raw) {
		try {
			LocalDate date = LocalDate.parse(raw);
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);
			return formatter.format(date);
		} catch (Exception ignored) {
			return null;
		}
	}

	private record StudentAnnouncementRow(
			String title,
			String content,
			String postedOn,
			String professorName,
			String subject,
			String imageUrl
	) {
	}
}
