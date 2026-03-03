package com.example.Gnosis.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
	@GetMapping({"/", "/landingPage"})
	public String landingPage() {
		return "Landing.html";
	}

	@GetMapping("/wala")
	public String walaPage(Authentication authentication, HttpSession session, Model model) {
		model.addAttribute("studentId", authentication != null ? authentication.getName() : "anonymous");
		model.addAttribute("sessionId", session.getId());
		model.addAttribute("createdAt", session.getCreationTime());
		model.addAttribute("lastAccessedAt", session.getLastAccessedTime());
		model.addAttribute("maxInactiveInterval", session.getMaxInactiveInterval());
		return "wala.html";
	}
}
