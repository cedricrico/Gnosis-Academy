package com.example.Gnosis.web;

import com.example.Gnosis.professor.ProfessorService;
import com.example.Gnosis.schoolclass.SchoolClassService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfessorController {
	private final ProfessorService professorService;
	private final SchoolClassService schoolClassService;

	public ProfessorController(ProfessorService professorService, SchoolClassService schoolClassService) {
		this.professorService = professorService;
		this.schoolClassService = schoolClassService;
	}

	@GetMapping({"/Professor-login", "/loginprofessor"})
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

	@GetMapping({"/Professor-registration", "/registerprofessor"})
	public String registerProfessorPage() {
		return "registerprofessor";
	}

	@GetMapping({"/Professor-landing", "/professor-landing"})
	public String professorLandingPage() {
		return "instructor-landing";
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
			return "redirect:/Professor-login?registered";
		} catch (IllegalArgumentException e) {
			redirectAttributes.addFlashAttribute("professorRegisterError", e.getMessage());
			return "redirect:/Professor-registration";
		}
	}

	@GetMapping("/professor/home")
	public String professorHome(Authentication authentication, HttpSession session, Model model) {
		String professorId = authentication != null ? authentication.getName() : "unknown";
		String professorName = professorId;

		if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
			professorId = userDetails.getUsername();
		}
		// If our custom principal is used, expose the full name in the UI.
		if (authentication != null && authentication.getPrincipal() instanceof com.example.Gnosis.security.ProfessorUserDetails pud) {
			professorId = pud.getProfessorId();
			professorName = pud.getFullName();
		}

		model.addAttribute("professorId", professorId);
		model.addAttribute("professorName", professorName);
		model.addAttribute("sessionId", session.getId());
		model.addAttribute("assignedClasses", schoolClassService.findForProfessor(professorId, professorName));
		return "professor-home";
	}



	@GetMapping("/Professor-landing, /professor-landing")
	public String Professor_landing_page(){
		return "instructor-landing";

		
	}

}
