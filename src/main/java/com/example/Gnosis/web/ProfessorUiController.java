package com.example.Gnosis.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.core.Authentication;
import com.example.Gnosis.schoolclass.SchoolClassDto;
import com.example.Gnosis.schoolclass.SchoolClassService;
import com.example.Gnosis.assignment.AssignmentRepository;
import com.example.Gnosis.user.User;
import com.example.Gnosis.user.UserRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/professor")
public class ProfessorUiController {
	private final SchoolClassService schoolClassService;
	private final UserRepository userRepository;
	private final AssignmentRepository assignmentRepository;

	public ProfessorUiController(SchoolClassService schoolClassService, UserRepository userRepository, AssignmentRepository assignmentRepository) {
		this.schoolClassService = schoolClassService;
		this.userRepository = userRepository;
		this.assignmentRepository = assignmentRepository;
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
		long assignmentCount = assignmentRepository.countByProfessorId(professorId);
		model.addAttribute("professorAssignmentCount", assignmentCount);
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
			String courseCode = safeTrim(cls.getCourseCode());
			String section = safeTrim(cls.getSectionName());
			if (course == null || section == null) {
				continue;
			}
			String key = (course + "||" + section).toLowerCase();
			SectionSummaryBuilder builder = grouped.computeIfAbsent(key,
					ignored -> new SectionSummaryBuilder(section, course));
			if (builder.courseCode == null) {
				builder.courseCode = courseCode;
			}

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
			long studentCount = countUniqueStudents(
					findStudentsForSection(builder.department, builder.courseCode, builder.sectionName)
			);
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

	@GetMapping("/assignment-submissions")
	public String assignmentSubmissions() {
		return "professor/assignment-submissions";
	}

	@GetMapping("/lessons")
	public String lessons() {
		return "professor/lessons";
	}

	@GetMapping("/quiz")
	public String quiz() {
		return "professor/quiz";
	}

	@GetMapping("/quiz-results")
	public String quizResults() {
		return "professor/quiz-results";
	}

	@GetMapping("/subjects")
	public String subjects(Authentication authentication, Model model, @org.springframework.web.bind.annotation.RequestParam(name = "section", required = false) String section) {
		String professorId = authentication != null ? authentication.getName() : "";
		String professorFullName = professorId;
		if (authentication != null && authentication.getPrincipal() instanceof com.example.Gnosis.security.ProfessorUserDetails pud) {
			professorId = pud.getEmployeeId();
			professorFullName = pud.getFullName();
		}

		String sectionFilter = safeTrim(section);
		List<SchoolClassDto> classes = schoolClassService.findForProfessor(professorId, professorFullName);
		List<SubjectRow> subjectRows = new ArrayList<>();

		for (SchoolClassDto cls : classes) {
			String sectionName = safeTrim(cls.getSectionName());
			if (sectionFilter != null) {
				if (sectionName == null) {
					continue;
				}
				String courseName = safeTrim(cls.getCourseName());
				String courseCode = safeTrim(cls.getCourseCode());
				if (!matchesSectionFilter(sectionFilter, courseName, courseCode, sectionName)) {
					continue;
				}
			}

			for (SchoolClassDto.SubjectDto subject : cls.getSubjects()) {
				String code = safeTrim(subject.getCode());
				String name = safeTrim(subject.getName());
				String schedule = formatSchedule(subject.getSchedule());
				if (code == null && name == null) {
					continue;
				}
				subjectRows.add(new SubjectRow(code != null ? code : "-", name != null ? name : "-", "-", schedule));
			}
		}

		model.addAttribute("professorSubjects", subjectRows);
		model.addAttribute("selectedSection", sectionFilter);
		return "professor/subjects";
	}

	@GetMapping("/masterlist")
	public String masterlist(Authentication authentication, Model model, @org.springframework.web.bind.annotation.RequestParam(name = "section", required = false) String section) {
		String professorId = authentication != null ? authentication.getName() : "";
		String professorFullName = professorId;
		if (authentication != null && authentication.getPrincipal() instanceof com.example.Gnosis.security.ProfessorUserDetails pud) {
			professorId = pud.getEmployeeId();
			professorFullName = pud.getFullName();
		}

		String sectionFilter = safeTrim(section);
		List<SchoolClassDto> classes = schoolClassService.findForProfessor(professorId, professorFullName);
		Map<String, User> studentsById = new LinkedHashMap<>();

		if (sectionFilter == null) {
			for (SchoolClassDto cls : classes) {
				String candidate = safeTrim(cls.getSectionName());
				if (candidate != null) {
					String encoded = URLEncoder.encode(candidate, StandardCharsets.UTF_8);
					return "redirect:/professor/masterlist?section=" + encoded;
				}
			}
		}

		if (sectionFilter == null) {
			model.addAttribute("professorMasterlistStudents", List.of());
			model.addAttribute("masterlistCount", 0);
			model.addAttribute("selectedSection", null);
			return "professor/masterlist";
		}

		for (SchoolClassDto cls : classes) {
			String courseName = safeTrim(cls.getCourseName());
			String courseCode = safeTrim(cls.getCourseCode());
			String sectionName = safeTrim(cls.getSectionName());
			if (courseName == null || sectionName == null) {
				continue;
			}
			if (!matchesSectionFilter(sectionFilter, courseName, courseCode, sectionName)) {
				continue;
			}

			List<User> students = findStudentsForSection(courseName, courseCode, sectionName);
			for (User student : students) {
				String studentId = safeTrim(student.getStudentId());
				if (studentId != null) {
					studentsById.putIfAbsent(studentId, student);
				}
			}
		}

		List<MasterlistStudentRow> masterlist = studentsById.values().stream()
				.sorted((left, right) -> {
					int last = compareNullable(left.getLastName(), right.getLastName());
					if (last != 0) {
						return last;
					}
					int first = compareNullable(left.getFirstName(), right.getFirstName());
					if (first != 0) {
						return first;
					}
					return compareNullable(left.getStudentId(), right.getStudentId());
				})
				.map(student -> new MasterlistStudentRow(
						safeTrim(student.getStudentId()) != null ? student.getStudentId().trim() : "-",
						buildStudentName(student),
						"-",
						safeTrim(student.getStatus()) != null ? student.getStatus().trim() : "Active",
						"-"
				))
				.toList();

		model.addAttribute("professorMasterlistStudents", masterlist);
		model.addAttribute("masterlistCount", masterlist.size());
		model.addAttribute("selectedSection", sectionFilter);
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
		private String courseCode;

		private SectionSummaryBuilder(String sectionName, String department) {
			this.sectionName = sectionName;
			this.department = department;
		}
	}

	private record SubjectRow(String code, String name, String units, String schedule) {
	}

	private static String formatSchedule(List<SchoolClassDto.ScheduleSlotDto> slots) {
		if (slots == null || slots.isEmpty()) {
			return "-";
		}
		List<String> formatted = new ArrayList<>();
		for (SchoolClassDto.ScheduleSlotDto slot : slots) {
			if (slot == null) {
				continue;
			}
			String days = slot.getDays() != null && !slot.getDays().isEmpty()
					? String.join(", ", slot.getDays())
					: "";
			String start = safeTrim(slot.getStartTime());
			String end = safeTrim(slot.getEndTime());
			String time = (start != null && end != null) ? (start + "-" + end) : "";
			String combined;
			if (!days.isEmpty() && !time.isEmpty()) {
				combined = days + " " + time;
			} else if (!days.isEmpty()) {
				combined = days;
			} else if (!time.isEmpty()) {
				combined = time;
			} else {
				continue;
			}
			formatted.add(combined);
		}
		return formatted.isEmpty() ? "-" : String.join("; ", formatted);
	}

	private record MasterlistStudentRow(String studentId, String fullName, String email, String status, String averageGrade) {
	}

	private static String buildStudentName(User student) {
		String last = safeTrim(student.getLastName());
		String first = safeTrim(student.getFirstName());
		String middle = safeTrim(student.getMiddleInitial());
		StringBuilder builder = new StringBuilder();
		if (last != null) {
			builder.append(last);
		}
		if (first != null) {
			if (!builder.isEmpty()) {
				builder.append(", ");
			}
			builder.append(first);
		}
		if (middle != null) {
			builder.append(" ").append(middle);
		}
		return builder.isEmpty() ? "-" : builder.toString();
	}

	private static int compareNullable(String left, String right) {
		if (left == null && right == null) {
			return 0;
		}
		if (left == null) {
			return 1;
		}
		if (right == null) {
			return -1;
		}
		return left.compareToIgnoreCase(right);
	}

	private static boolean matchesSectionFilter(String sectionFilter, String courseName, String courseCode, String sectionName) {
		if (sectionFilter == null) {
			return true;
		}
		String normalizedFilter = normalizeKey(sectionFilter);
		String normalizedSection = normalizeKey(sectionName);
		if (sectionMatchesFilter(normalizedFilter, normalizedSection)) {
			return true;
		}
		String normalizedCombined = normalizeKey(courseName + " " + sectionName);
		String normalizedCombinedCode = courseCode != null ? normalizeKey(courseCode + " " + sectionName) : "";
		return normalizedFilter.equals(normalizedCombined)
				|| (!normalizedCombinedCode.isEmpty() && normalizedFilter.equals(normalizedCombinedCode));
	}

	private List<User> findStudentsForSection(String courseName, String courseCode, String sectionName) {
		List<User> combined = new ArrayList<>();
		Set<String> seen = new HashSet<>();

		String combinedCourseName = courseName + " " + sectionName;
		String combinedCourseCode = courseCode != null ? courseCode + " " + sectionName : null;

		List<String> courseVariants = new ArrayList<>();
		courseVariants.add(courseName);
		if (courseCode != null && !courseCode.equalsIgnoreCase(courseName)) {
			courseVariants.add(courseCode);
		}
		courseVariants.add(combinedCourseName);
		if (combinedCourseCode != null && !combinedCourseCode.equalsIgnoreCase(combinedCourseName)) {
			courseVariants.add(combinedCourseCode);
		}

		List<String> sectionVariants = buildSectionVariants(sectionName, courseName, courseCode);

		for (String courseVariant : courseVariants) {
			if (courseVariant == null || courseVariant.isBlank()) {
				continue;
			}
			for (String sectionVariant : sectionVariants) {
				List<User> students = userRepository
						.findByCourseIgnoreCaseAndSectionNameIgnoreCaseOrderByLastNameAscFirstNameAsc(courseVariant, sectionVariant);
				for (User student : students) {
					String id = safeTrim(student.getStudentId());
					if (id == null || seen.contains(id)) {
						continue;
					}
					seen.add(id);
					combined.add(student);
				}
			}
		}

		for (String sectionVariant : sectionVariants) {
			List<User> bySectionOnly = userRepository
					.findBySectionNameIgnoreCaseOrderByLastNameAscFirstNameAsc(sectionVariant);
			for (User student : bySectionOnly) {
				String id = safeTrim(student.getStudentId());
				if (id == null || seen.contains(id)) {
					continue;
				}
				seen.add(id);
				combined.add(student);
			}
		}

		String normalizedCourseName = normalizeKey(courseName);
		String normalizedCourseCode = courseCode != null ? normalizeKey(courseCode) : "";
		boolean hasCourseName = !normalizedCourseName.isEmpty();
		boolean hasCourseCode = !normalizedCourseCode.isEmpty();

		List<User> byCourseContainingSection = userRepository
				.findByCourseIgnoreCaseContainingOrderByLastNameAscFirstNameAsc(sectionName);
		for (User student : byCourseContainingSection) {
			String id = safeTrim(student.getStudentId());
			if (id == null || seen.contains(id)) {
				continue;
			}
			String studentCourse = normalizeKey(student.getCourse());
			boolean sectionMatch = false;
			for (String sectionVariant : sectionVariants) {
				String normalizedSection = normalizeKey(sectionVariant);
				if (!normalizedSection.isEmpty() && studentCourse.contains(normalizedSection)) {
					sectionMatch = true;
					break;
				}
			}
			if (!sectionMatch) {
				continue;
			}
			if (hasCourseName || hasCourseCode) {
				boolean nameMatches = hasCourseName && studentCourse.contains(normalizedCourseName);
				boolean codeMatches = hasCourseCode && studentCourse.contains(normalizedCourseCode);
				if (!nameMatches && !codeMatches) {
					continue;
				}
			}
			seen.add(id);
			combined.add(student);
		}

		return combined;
	}

	private static List<String> buildSectionVariants(String sectionName, String courseName, String courseCode) {
		LinkedHashSet<String> variants = new LinkedHashSet<>();
		String base = safeTrim(sectionName);
		if (base != null) {
			variants.add(base);
		}
		String courseCodeTrimmed = safeTrim(courseCode);
		String courseNameTrimmed = safeTrim(courseName);
		if (base != null && courseCodeTrimmed != null) {
			variants.add(courseCodeTrimmed + " " + base);
		}
		if (base != null && courseNameTrimmed != null) {
			variants.add(courseNameTrimmed + " " + base);
		}
		String stripped = stripCoursePrefix(base, courseCode);
		if (stripped != null) {
			variants.add(stripped);
		}
		String strippedName = stripCoursePrefix(base, courseName);
		if (strippedName != null) {
			variants.add(strippedName);
		}
		return variants.isEmpty() ? List.of() : new ArrayList<>(variants);
	}

	private static String stripCoursePrefix(String sectionName, String coursePrefix) {
		if (sectionName == null || coursePrefix == null) {
			return null;
		}
		String sectionTrimmed = sectionName.trim();
		String prefixTrimmed = coursePrefix.trim();
		if (sectionTrimmed.isEmpty() || prefixTrimmed.isEmpty()) {
			return null;
		}
		if (!sectionTrimmed.regionMatches(true, 0, prefixTrimmed, 0, prefixTrimmed.length())) {
			return null;
		}
		String remainder = sectionTrimmed.substring(prefixTrimmed.length());
		remainder = remainder.replaceFirst("^[\\s\\-_/]+", "");
		return safeTrim(remainder);
	}

	private static String normalizeKey(String value) {
		if (value == null) {
			return "";
		}
		String trimmed = value.trim();
		if (trimmed.isEmpty()) {
			return "";
		}
		String normalized = trimmed.replace('\\', ' ')
				.replace('/', ' ')
				.replace('-', ' ')
				.replace('_', ' ');
		return normalized.replaceAll("\\s+", " ").toLowerCase();
	}

	private static boolean sectionMatchesFilter(String normalizedFilter, String normalizedSection) {
		if (normalizedSection.isEmpty() || normalizedFilter.isEmpty()) {
			return false;
		}
		if (normalizedFilter.equals(normalizedSection)) {
			return true;
		}
		return normalizedFilter.endsWith(" " + normalizedSection);
	}

	private static long countUniqueStudents(List<User> students) {
		Set<String> unique = new HashSet<>();
		for (User student : students) {
			String id = safeTrim(student.getStudentId());
			if (id != null) {
				unique.add(id);
			}
		}
		return unique.size();
	}
}

