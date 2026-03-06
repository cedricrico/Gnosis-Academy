package com.example.Gnosis.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminController {
	@GetMapping("/admin")
	public String adminRoot() {
		return "redirect:/admin/dashboard";
	}

	@GetMapping("/admin-login")
	public String adminLoginPage(
			@RequestParam(value = "error", required = false) String error,
			@RequestParam(value = "logout", required = false) String logout,
			Model model
	) {
		if (error != null) {
			model.addAttribute("loginError", "Invalid admin username or password");
		}
		if (logout != null) {
			model.addAttribute("loginInfo", "You have been logged out.");
		}
		return "admin-login";
	}

	@GetMapping("/admin/dashboard")
	public String adminDashboard() {
		return "Admin-dashboard";
	}
}
