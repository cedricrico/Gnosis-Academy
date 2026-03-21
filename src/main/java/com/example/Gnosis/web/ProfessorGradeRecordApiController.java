package com.example.Gnosis.web;

import com.example.Gnosis.grade.ProfessorGradeRecordService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/professor/api/grade-records")
public class ProfessorGradeRecordApiController {
	private final ProfessorGradeRecordService professorGradeRecordService;

	public ProfessorGradeRecordApiController(ProfessorGradeRecordService professorGradeRecordService) {
		this.professorGradeRecordService = professorGradeRecordService;
	}

	@GetMapping
	public ProfessorGradeRecordService.GradeRecordResponse list(Authentication authentication) {
		ProfessorIdentity professor = resolveProfessor(authentication);
		return professorGradeRecordService.listGradeRecords(professor.id(), professor.name());
	}

	private static ProfessorIdentity resolveProfessor(Authentication authentication) {
		if (authentication != null && authentication.getPrincipal() instanceof com.example.Gnosis.security.ProfessorUserDetails pud) {
			return new ProfessorIdentity(pud.getEmployeeId(), pud.getFullName());
		}
		String fallback = authentication != null ? authentication.getName() : "unknown";
		return new ProfessorIdentity(fallback, fallback);
	}

	private record ProfessorIdentity(String id, String name) {
	}
}
