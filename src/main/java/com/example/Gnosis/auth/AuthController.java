package com.example.Gnosis.auth;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {
	private final UserService userService;

	public AuthController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping({"/Student-registration", "/registrationPage", "/Register", "/Register.html"})
	public String registrationPage(Model model) {
		if (!model.containsAttribute("registerRequest")) {
			model.addAttribute("registerRequest", new RegisterRequest());
		}
		return "Register";
	}

	@PostMapping("/register")
	public String register(
			@Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
			BindingResult bindingResult,
			Model model
	) {
		if (bindingResult.hasErrors()) {
			return "Register";
		}

		try {
			userService.register(registerRequest);
		} catch (IllegalArgumentException e) {
			model.addAttribute("registerError", e.getMessage());
			return "Register";
		}

		return "redirect:/Student-login?registered";
	}

	@GetMapping({"/Student-login", "/loginPage", "/Login", "/Login.html"})
	public String loginPage(
			@RequestParam(value = "error", required = false) String error,
			@RequestParam(value = "registered", required = false) String registered,
			@RequestParam(value = "logout", required = false) String logout,
			Model model
	) {
		if (error != null) {
			model.addAttribute("loginError", "Invalid student ID or password");
		}
		if (registered != null) {
			model.addAttribute("loginInfo", "Account created. Please log in.");
		}
		if (logout != null) {
			model.addAttribute("loginInfo", "You have been logged out.");
		}
		return "Login";
	}

}
