package com.example.Gnosis.web;

import com.example.Gnosis.professor.ProfessorService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfessorController {
	private final ProfessorService professorService;

	public ProfessorController(ProfessorService professorService) {
		this.professorService = professorService;
	}

	@GetMapping("/loginprofessor")
	public String loginProfessorPage(
			@RequestParam(value = "error", required = false) String error,
			@RequestParam(value = "registered", required = false) String registered,
			@RequestParam(value = "logout", required = false) String logout,
			Model model
	) {
		if (error != null) {
			model.addAttribute("professorLoginError", "Invalid professor ID or password");
		}
		if (registered != null) {
			model.addAttribute("professorLoginInfo", "Professor account created. Please log in.");
		}
		if (logout != null) {
			model.addAttribute("professorLoginInfo", "You have been logged out.");
		}
		return "loginprofessor";
	}

	@GetMapping("/registerprofessor")
	public String registerProfessorPage() {
		return "registerprofessor";
	}

	@PostMapping("/professor/register")
	public String registerProfessor(
			@RequestParam("professorId") String professorId,
			@RequestParam("firstName") String firstName,
			@RequestParam(value = "middleInitial", required = false) String middleInitial,
			@RequestParam("lastName") String lastName,
			@RequestParam("age") Integer age,
			@RequestParam("sex") String sex,
			@RequestParam("department") String department,
			@RequestParam("position") String position,
			@RequestParam("password") String password,
			@RequestParam("confirmPassword") String confirmPassword,
			RedirectAttributes redirectAttributes
	) {
		try {
			professorService.register(
					professorId,
					firstName,
					middleInitial,
					lastName,
					age,
					sex,
					department,
					position,
					password,
					confirmPassword
			);
			return "redirect:/loginprofessor?registered";
		} catch (IllegalArgumentException e) {
			redirectAttributes.addFlashAttribute("professorRegisterError", e.getMessage());
			return "redirect:/registerprofessor";
		}
	}

	@PostMapping("/professor/login")
	public String loginProfessor(
			@RequestParam("professorId") String professorId,
			@RequestParam("password") String password,
			HttpSession session
	) {
		return professorService.authenticate(professorId, password)
				.map(professor -> {
					session.setAttribute("professorAuth", true);
					session.setAttribute("professorId", professor.getProfessorId());
					session.setAttribute("professorName", professor.getFirstName() + " " + professor.getLastName());
					return "redirect:/professor/home";
				})
				.orElse("redirect:/loginprofessor?error");
	}

	@GetMapping("/professor/home")
	public String professorHome(HttpSession session, Model model) {
		Boolean professorAuth = (Boolean) session.getAttribute("professorAuth");
		if (professorAuth == null || !professorAuth) {
			return "redirect:/loginprofessor?error";
		}
		model.addAttribute("professorId", session.getAttribute("professorId"));
		model.addAttribute("professorName", session.getAttribute("professorName"));
		model.addAttribute("sessionId", session.getId());
		return "professor-home";
	}

	@PostMapping("/professor/logout")
	public String professorLogout(HttpSession session) {
		session.invalidate();
		return "redirect:/loginprofessor?logout";
	}
}
