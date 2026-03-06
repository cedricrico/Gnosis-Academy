package com.example.Gnosis.web;

import com.example.Gnosis.schoolclass.SchoolClassService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
	private final SchoolClassService schoolClassService;

	public PageController(SchoolClassService schoolClassService) {
		this.schoolClassService = schoolClassService;
	}

	@GetMapping({"/", "/landingPage"})
	public String landingPage() {
		return "Landing.html";
	}

	@GetMapping("/student/home")
	public String walaPage(Authentication authentication, HttpSession session, Model model) {
		model.addAttribute("studentId", authentication != null ? authentication.getName() : "anonymous");
		model.addAttribute("sessionId", session.getId());
		model.addAttribute("createdAt", session.getCreationTime());
		model.addAttribute("lastAccessedAt", session.getLastAccessedTime());
		model.addAttribute("maxInactiveInterval", session.getMaxInactiveInterval());
		model.addAttribute("classes", schoolClassService.findForStudents());
		return "wala.html";
	}
}
