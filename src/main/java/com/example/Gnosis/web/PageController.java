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
		return "landing";
	}

	@GetMapping("/favicon.ico")
	public String favicon() {
		return "redirect:/assets/img/office-building.png";
	}

	@GetMapping("/student/home")
	public String walaPage(Authentication authentication, HttpSession session, Model model) {
		// Backward compatible route: older configs may still redirect here after login.
		// The new student UI lives at /student/dashboard.
		return "redirect:/student/dashboard";
	}
}
