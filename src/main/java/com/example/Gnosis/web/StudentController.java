package com.example.Gnosis.web;

import com.example.Gnosis.user.User;
import com.example.Gnosis.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/student")
public class StudentController {
	private final UserRepository userRepository;

	public StudentController(UserRepository userRepository) {
		this.userRepository = userRepository;
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
	public String studentDashboard() {
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

	@GetMapping("/quiz")
	public String studentQuiz() {
		return "student/student-quiz";
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
}
