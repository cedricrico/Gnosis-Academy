package com.example.Gnosis.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/professor")
public class ProfessorUiController {
	@GetMapping({"", "/", "/dashboard"})
	public String dashboard() {
		return "professor/instructor-dashboard";
	}

	@GetMapping("/sections")
	public String sections() {
		return "professor/sections";
	}

	@GetMapping("/profile")
	public String profile() {
		return "professor/instructor-profile";
	}

	@GetMapping("/announcements")
	public String announcements() {
		return "professor/announcements";
	}

	@GetMapping("/assignments")
	public String assignments() {
		return "professor/assignments";
	}

	@GetMapping("/lessons")
	public String lessons() {
		return "professor/lessons";
	}

	@GetMapping("/quiz")
	public String quiz() {
		return "professor/quiz";
	}

	@GetMapping("/subjects")
	public String subjects() {
		return "professor/subjects";
	}

	@GetMapping("/masterlist")
	public String masterlist() {
		return "professor/masterlist";
	}

	@GetMapping("/grade-records")
	public String gradeRecords() {
		return "professor/grade-records";
	}
}

