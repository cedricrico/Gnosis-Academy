package com.example.Gnosis.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.core.Authentication;
import com.example.Gnosis.schoolclass.SchoolClassDto;
import com.example.Gnosis.schoolclass.SchoolClassService;
import com.example.Gnosis.user.User;
import com.example.Gnosis.user.UserRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/professor")
public class ProfessorUiController {
	private final SchoolClassService schoolClassService;
	private final UserRepository userRepository;

	public ProfessorUiController(SchoolClassService schoolClassService, UserRepository userRepository) {
		this.schoolClassService = schoolClassService;
		this.userRepository = userRepository;
	}

	@ModelAttribute
	public void addProfessorTemplateModel(Authentication authentication, Model model) {
		if (authentication == null) {
			return;
		}
		String professorId = authentication.getName();
		String professorFullName = professorId;

		if (authentication.getPrincipal() instanceof com.example.Gnosis.security.ProfessorUserDetails pud) {
			professorId = pud.getEmployeeId();
			professorFullName = pud.getFullName();
		}

		model.addAttribute("professorId", professorId);
		model.addAttribute("professorFullName", professorFullName);

		List<SchoolClassDto> classes = schoolClassService.findForProfessor(professorId, professorFullName);
		model.addAttribute("professorAnnouncementTargets", buildAnnouncementTargets(classes));

		long sectionCount = classes.stream()
				.map(SchoolClassDto::getSectionName)
				.filter(value -> value != null && !value.isBlank())
				.map(String::trim)
				.distinct()
				.count();

		long subjectCount = classes.stream()
				.flatMap(cls -> cls.getSubjects().stream())
				.map(subject -> subject.getCode() != null && !subject.getCode().isBlank()
						? subject.getCode().trim()
						: subject.getName() != null ? subject.getName().trim() : "")
				.filter(value -> !value.isBlank())
				.distinct()
				.count();

		Set<String> uniqueStudentIds = new HashSet<>();
		for (SchoolClassDto cls : classes) {
			String course = cls.getCourseName();
			String section = cls.getSectionName();
			if (course == null || course.isBlank() || section == null || section.isBlank()) {
				continue;
			}
			List<User> students = userRepository
					.findByCourseIgnoreCaseAndSectionNameIgnoreCaseOrderByLastNameAscFirstNameAsc(course.trim(), section.trim());
			for (User student : students) {
				if (student.getStudentId() != null && !student.getStudentId().isBlank()) {
					uniqueStudentIds.add(student.getStudentId().trim());
				}
			}
		}

		model.addAttribute("professorSectionCount", sectionCount);
		model.addAttribute("professorSubjectCount", subjectCount);
		model.addAttribute("professorStudentCount", uniqueStudentIds.size());
		model.addAttribute("professorAssignmentCount", 0);
	}

	@GetMapping({"", "/", "/dashboard"})
	public String dashboard() {
		return "professor/instructor-dashboard";
	}

	@GetMapping("/sections")
	public String sections(Authentication authentication, Model model) {
		String professorId = authentication != null ? authentication.getName() : "";
		String professorFullName = professorId;
		if (authentication != null && authentication.getPrincipal() instanceof com.example.Gnosis.security.ProfessorUserDetails pud) {
			professorId = pud.getEmployeeId();
			professorFullName = pud.getFullName();
		}

		List<SchoolClassDto> classes = schoolClassService.findForProfessor(professorId, professorFullName);
		Map<String, SectionSummaryBuilder> grouped = new LinkedHashMap<>();

		for (SchoolClassDto cls : classes) {
			String course = safeTrim(cls.getCourseName());
			String section = safeTrim(cls.getSectionName());
			if (course == null || section == null) {
				continue;
			}
			String key = (course + "||" + section).toLowerCase();
			SectionSummaryBuilder builder = grouped.computeIfAbsent(key,
					ignored -> new SectionSummaryBuilder(section, course));

			cls.getSubjects().forEach(subject -> {
				String subjectLabel = safeTrim(subject.getName());
				if (subjectLabel == null) {
					subjectLabel = safeTrim(subject.getCode());
				}
				if (subjectLabel != null) {
					builder.subjects.add(subjectLabel);
				}
			});
		}

		List<SectionSummary> summaries = new ArrayList<>();
		for (SectionSummaryBuilder builder : grouped.values()) {
			long studentCount = userRepository
					.findByCourseIgnoreCaseAndSectionNameIgnoreCaseOrderByLastNameAscFirstNameAsc(
							builder.department,
							builder.sectionName
					)
					.size();
			List<String> subjectList = builder.subjects.stream().sorted().toList();
			summaries.add(new SectionSummary(builder.sectionName, builder.department, studentCount, subjectList));
		}

		model.addAttribute("professorSections", summaries);
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

	private static String safeTrim(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private record SectionSummary(String sectionName, String department, long studentCount, List<String> subjects) {
	}

	private static List<String> buildAnnouncementTargets(List<SchoolClassDto> classes) {
		List<String> targets = new ArrayList<>();
		targets.add("All Sections");
		Set<String> seen = new HashSet<>();
		for (SchoolClassDto cls : classes) {
			String section = safeTrim(cls.getSectionName());
			if (section == null) {
				continue;
			}
			String key = section.toLowerCase();
			if (seen.add(key)) {
				targets.add(section);
			}
		}
		return targets;
	}

	private static class SectionSummaryBuilder {
		private final String sectionName;
		private final String department;
		private final Set<String> subjects = new HashSet<>();

		private SectionSummaryBuilder(String sectionName, String department) {
			this.sectionName = sectionName;
			this.department = department;
		}
	}
}

