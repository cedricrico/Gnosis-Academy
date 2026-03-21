package com.example.Gnosis.web;

import com.example.Gnosis.quiz.ProfessorQuizResultsService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
public class QuizApiController {
	private final ProfessorQuizResultsService professorQuizResultsService;

	public QuizApiController(ProfessorQuizResultsService professorQuizResultsService) {
		this.professorQuizResultsService = professorQuizResultsService;
	}

	@GetMapping
	public List<ProfessorQuizResultsService.QuizResultCard> list(Authentication authentication) {
		String professorId = requireProfessorId(authentication);
		return professorQuizResultsService.listQuizCards(professorId);
	}

	@GetMapping("/{quizId}/results")
	public ProfessorQuizResultsService.QuizResultDetails results(
			@PathVariable Long quizId,
			Authentication authentication
	) {
		String professorId = requireProfessorId(authentication);
		return professorQuizResultsService.getQuizResults(quizId, professorId);
	}

	private static String requireProfessorId(Authentication authentication) {
		if (authentication != null && authentication.getPrincipal() instanceof com.example.Gnosis.security.ProfessorUserDetails pud) {
			return pud.getEmployeeId();
		}
		if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
			throw new IllegalArgumentException("Professor is required.");
		}
		return authentication.getName().trim();
	}
}
