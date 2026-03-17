package com.example.Gnosis.web;

import com.example.Gnosis.quiz.QuizRequest;
import com.example.Gnosis.quiz.QuizResponse;
import com.example.Gnosis.quiz.QuizService;
import com.example.Gnosis.schoolclass.SchoolClassDto;
import com.example.Gnosis.schoolclass.SchoolClassService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@RestController
@RequestMapping("/professor/api/quizzes")
public class ProfessorQuizApiController {
	private final SchoolClassService schoolClassService;
	private final QuizService quizService;

	public ProfessorQuizApiController(SchoolClassService schoolClassService, QuizService quizService) {
		this.schoolClassService = schoolClassService;
		this.quizService = quizService;
	}

	@GetMapping("/meta")
	public QuizMetaResponse meta(Authentication authentication) {
		ProfessorIdentity professor = resolveProfessor(authentication);
		List<SchoolClassDto> classes = schoolClassService.findForProfessor(professor.id(), professor.name());
		LinkedHashSet<String> sections = new LinkedHashSet<>();
		LinkedHashSet<String> subjects = new LinkedHashSet<>();

		for (SchoolClassDto schoolClass : classes) {
			String section = safeTrim(schoolClass.getSectionName());
			if (section != null) {
				sections.add(section);
			}

			for (SchoolClassDto.SubjectDto subject : schoolClass.getSubjects()) {
				String code = safeTrim(subject.getCode());
				String name = safeTrim(subject.getName());
				String label = code != null ? code : name;
				if (code != null && name != null) {
					label = code + " - " + name;
				}
				if (label != null) {
					subjects.add(label);
				}
			}
		}

		return new QuizMetaResponse(new ArrayList<>(sections), new ArrayList<>(subjects));
	}

	@GetMapping
	public List<QuizResponse> list(Authentication authentication) {
		ProfessorIdentity professor = resolveProfessor(authentication);
		return quizService.listForProfessor(professor.id());
	}

	@PostMapping
	public QuizResponse create(Authentication authentication, @RequestBody QuizRequest request) {
		ProfessorIdentity professor = resolveProfessor(authentication);
		return quizService.create(professor.id(), professor.name(), request);
	}

	@PutMapping("/{id}")
	public QuizResponse update(
			@PathVariable Long id,
			Authentication authentication,
			@RequestBody QuizRequest request
	) {
		ProfessorIdentity professor = resolveProfessor(authentication);
		return quizService.update(id, professor.id(), professor.name(), request);
	}

	@DeleteMapping("/{id}")
	public void delete(
			@PathVariable Long id,
			Authentication authentication
	) {
		ProfessorIdentity professor = resolveProfessor(authentication);
		quizService.delete(id, professor.id());
	}

	private static ProfessorIdentity resolveProfessor(Authentication authentication) {
		if (authentication != null && authentication.getPrincipal() instanceof com.example.Gnosis.security.ProfessorUserDetails pud) {
			return new ProfessorIdentity(pud.getEmployeeId(), pud.getFullName());
		}
		String fallback = authentication != null ? authentication.getName() : "unknown";
		return new ProfessorIdentity(fallback, fallback);
	}

	private record ProfessorIdentity(String id, String name) {}
	private record QuizMetaResponse(List<String> sections, List<String> subjects) {}

	private static String safeTrim(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
