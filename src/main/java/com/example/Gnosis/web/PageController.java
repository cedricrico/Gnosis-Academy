package com.example.Gnosis.web;

import com.example.Gnosis.schoolclass.SchoolClassService;
import com.example.Gnosis.user.User;
import com.example.Gnosis.user.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
	private final SchoolClassService schoolClassService;
	private final UserRepository userRepository;

	public PageController(SchoolClassService schoolClassService, UserRepository userRepository) {
		this.schoolClassService = schoolClassService;
		this.userRepository = userRepository;
	}

	@GetMapping({"/", "/landingPage"})
	public String landingPage() {
		return "Landing.html";
	}

	@GetMapping("/student/home")
	public String walaPage(Authentication authentication, HttpSession session, Model model) {
		String studentId = authentication != null ? authentication.getName() : "anonymous";
		model.addAttribute("studentId", studentId);
		model.addAttribute("sessionId", session.getId());
		model.addAttribute("createdAt", session.getCreationTime());
		model.addAttribute("lastAccessedAt", session.getLastAccessedTime());
		model.addAttribute("maxInactiveInterval", session.getMaxInactiveInterval());
		User user = userRepository.findByStudentId(studentId).orElse(null);
		String course = user != null ? user.getCourse() : null;
		String section = user != null ? user.getSectionName() : null;
		model.addAttribute("classes", schoolClassService.findForStudent(course, section));
		return "wala";
	}
}
