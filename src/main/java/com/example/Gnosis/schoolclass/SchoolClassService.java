package com.example.Gnosis.schoolclass;

import com.example.Gnosis.professor.ProfessorRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

@Service
public class SchoolClassService {
	private static final TypeReference<List<SchoolClassDto.SubjectDto>> SUBJECT_LIST_TYPE = new TypeReference<>() {
	};
	private static final Logger log = LoggerFactory.getLogger(SchoolClassService.class);

	private final SchoolClassRepository schoolClassRepository;
	private final ProfessorRepository professorRepository;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public SchoolClassService(
			SchoolClassRepository schoolClassRepository,
			ProfessorRepository professorRepository
	) {
		this.schoolClassRepository = schoolClassRepository;
		this.professorRepository = professorRepository;
	}

	@Transactional(readOnly = true)
	public List<SchoolClassDto> findAll() {
		try {
			return schoolClassRepository.findAllByOrderByCreatedAtDesc()
					.stream()
					.map(this::toDto)
					.toList();
		} catch (DataAccessException ex) {
			log.warn("Unable to load school classes from database. Returning empty list.", ex);
			return List.of();
		}
	}

	@Transactional(readOnly = true)
	public Optional<SchoolClassDto> findById(Long classId) {
		return schoolClassRepository.findById(classId).map(this::toDto);
	}

	@Transactional
	public SchoolClassDto create(SchoolClassDto payload) {
		SchoolClass schoolClass = new SchoolClass();
		applyPayload(schoolClass, payload);
		return toDto(schoolClassRepository.save(schoolClass));
	}

	@Transactional
	public SchoolClassDto update(Long classId, SchoolClassDto payload) {
		SchoolClass existing = schoolClassRepository.findById(classId)
				.orElseThrow(() -> new NoSuchElementException("Class not found: " + classId));
		applyPayload(existing, payload);
		return toDto(schoolClassRepository.save(existing));
	}

	@Transactional
	public void delete(Long classId) {
		if (!schoolClassRepository.existsById(classId)) {
			throw new NoSuchElementException("Class not found: " + classId);
		}
		schoolClassRepository.deleteById(classId);
	}

	@Transactional(readOnly = true)
	public List<SchoolClassDto> findForStudents() {
		return findAll();
	}

	@Transactional(readOnly = true)
	public List<SchoolClassDto> findForStudent(String course, String sectionName) {
		String courseNormalized = trimToNull(course);
		String sectionNormalized = trimToNull(sectionName);
		if (courseNormalized == null || sectionNormalized == null) {
			return List.of();
		}

		return findAll().stream()
				.filter(schoolClass -> courseMatches(schoolClass, courseNormalized)
						&& equalsIgnoreCase(trimToNull(schoolClass.getSectionName()), sectionNormalized))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<SchoolClassDto> findForProfessor(String professorId, String professorName) {
		String professorIdNormalized = trimToNull(professorId);
		String professorNameNormalized = trimToNull(professorName);

		return findAll().stream()
				.filter(schoolClass -> schoolClass.getSubjects().stream().anyMatch(subject -> {
					boolean idMatches = equalsIgnoreCase(trimToNull(subject.getInstructorId()), professorIdNormalized);
					boolean nameMatches = equalsIgnoreCase(trimToNull(subject.getInstructorName()), professorNameNormalized);
					return idMatches || nameMatches;
				}))
				.toList();
	}

	private void applyPayload(SchoolClass schoolClass, SchoolClassDto payload) {
		if (payload == null) {
			throw new IllegalArgumentException("Class payload is required.");
		}

		String courseCode = requireNonBlank(payload.getCourseCode(), "Course is required.");
		String courseName = requireNonBlank(payload.getCourseName(), "Course name is required.");
		String sectionName = requireNonBlank(payload.getSectionName(), "Section is required.");
		requireMaxLength(courseCode, 32, "Course code is too long.");
		requireMaxLength(courseName, 128, "Course name is too long.");
		requireMaxLength(sectionName, 128, "Section name is too long.");

		List<SchoolClassDto.SubjectDto> sanitizedSubjects = sanitizeSubjects(payload.getSubjects());
		String subjectsJson = toJson(sanitizedSubjects);
		SchoolClassDto.SubjectDto firstSubject = sanitizedSubjects.get(0);
		SchoolClassDto.ScheduleSlotDto firstSlot = firstSubject.getSchedule().get(0);
		String joinedDays = String.join(",", firstSlot.getDays());
		requireMaxLength(joinedDays, 64, "Schedule days are too long.");

		schoolClass.setCourseCode(courseCode);
		schoolClass.setCourseName(courseName);
		schoolClass.setCourse(courseName);
		schoolClass.setSectionName(sectionName);
		schoolClass.setSubjectName(firstSubject.getName());
		schoolClass.setSubjectCode(firstSubject.getCode());
		schoolClass.setProfessorId(firstSubject.getInstructorId());
		schoolClass.setScheduleDays(joinedDays);
		requireMaxLength(firstSlot.getStartTime(), 16, "Start time is too long.");
		requireMaxLength(firstSlot.getEndTime(), 16, "End time is too long.");
		schoolClass.setStartTime(firstSlot.getStartTime());
		schoolClass.setEndTime(firstSlot.getEndTime());
		schoolClass.setStatus("ACTIVE");
		schoolClass.setSubjectsJson(subjectsJson);
	}

	private List<SchoolClassDto.SubjectDto> sanitizeSubjects(List<SchoolClassDto.SubjectDto> subjects) {
		if (subjects == null || subjects.isEmpty()) {
			throw new IllegalArgumentException("At least one subject is required.");
		}

		List<SchoolClassDto.SubjectDto> sanitized = new ArrayList<>();
		Set<String> seenCodes = new HashSet<>();

		for (SchoolClassDto.SubjectDto subject : subjects) {
			if (subject == null) {
				throw new IllegalArgumentException("Invalid subject entry.");
			}

			SchoolClassDto.SubjectDto cleanSubject = new SchoolClassDto.SubjectDto();
			String subjectName = requireNonBlank(subject.getName(), "Subject name is required.");
			requireMaxLength(subjectName, 128, "Subject name is too long.");
			cleanSubject.setName(subjectName);
			String cleanCode = requireNonBlank(subject.getCode(), "Subject code is required.");
			requireMaxLength(cleanCode, 64, "Subject code is too long.");
			cleanSubject.setCode(cleanCode);
			String instructorId = requireNonBlank(subject.getInstructorId(), "Instructor is required.");
			requireMaxLength(instructorId, 32, "Instructor ID is too long.");
			if (!professorRepository.existsByEmployeeId(instructorId)) {
				throw new IllegalArgumentException("Instructor not found: " + instructorId);
			}
			cleanSubject.setInstructorId(instructorId);
			String instructorName = requireNonBlank(subject.getInstructorName(), "Instructor is required.");
			requireMaxLength(instructorName, 128, "Instructor name is too long.");
			cleanSubject.setInstructorName(instructorName);

			String codeKey = cleanCode.toUpperCase(Locale.ROOT);
			if (!seenCodes.add(codeKey)) {
				throw new IllegalArgumentException("Duplicate subject code: " + cleanCode);
			}

			List<SchoolClassDto.ScheduleSlotDto> schedule = sanitizeSchedule(subject.getSchedule(), cleanCode);
			cleanSubject.setSchedule(schedule);

			sanitized.add(cleanSubject);
		}

		return sanitized;
	}

	private List<SchoolClassDto.ScheduleSlotDto> sanitizeSchedule(List<SchoolClassDto.ScheduleSlotDto> slots, String subjectCode) {
		if (slots == null || slots.isEmpty()) {
			throw new IllegalArgumentException("Schedule is required for subject " + subjectCode + ".");
		}

		List<SchoolClassDto.ScheduleSlotDto> cleanedSlots = new ArrayList<>();
		for (SchoolClassDto.ScheduleSlotDto slot : slots) {
			if (slot == null) {
				throw new IllegalArgumentException("Invalid schedule entry for subject " + subjectCode + ".");
			}

			List<String> cleanedDays = new ArrayList<>();
			List<String> slotDays = slot.getDays();
			if (slotDays == null) {
				throw new IllegalArgumentException("Please select at least one day for subject " + subjectCode + ".");
			}
			for (String day : slotDays) {
				String dayValue = trimToNull(day);
				if (dayValue != null) {
					cleanedDays.add(dayValue);
				}
			}

			if (cleanedDays.isEmpty()) {
				throw new IllegalArgumentException("Please select at least one day for subject " + subjectCode + ".");
			}

			String startTime = requireNonBlank(slot.getStartTime(), "Start time is required for subject " + subjectCode + ".");
			String endTime = requireNonBlank(slot.getEndTime(), "End time is required for subject " + subjectCode + ".");
			if (startTime.compareTo(endTime) >= 0) {
				throw new IllegalArgumentException("End time must be after start time for subject " + subjectCode + ".");
			}
			requireMaxLength(startTime, 16, "Start time is too long for subject " + subjectCode + ".");
			requireMaxLength(endTime, 16, "End time is too long for subject " + subjectCode + ".");

			SchoolClassDto.ScheduleSlotDto cleanSlot = new SchoolClassDto.ScheduleSlotDto();
			cleanSlot.setDays(cleanedDays);
			cleanSlot.setStartTime(startTime);
			cleanSlot.setEndTime(endTime);
			cleanedSlots.add(cleanSlot);
		}

		return cleanedSlots;
	}

	private SchoolClassDto toDto(SchoolClass schoolClass) {
		SchoolClassDto dto = new SchoolClassDto();
		dto.setId(schoolClass.getId());
		dto.setCourseCode(schoolClass.getCourseCode());
		String courseName = trimToNull(schoolClass.getCourseName());
		dto.setCourseName(courseName != null ? courseName : trimToNull(schoolClass.getCourse()));
		dto.setSectionName(schoolClass.getSectionName());
		dto.setCreatedAt(schoolClass.getCreatedAt());
		dto.setUpdatedAt(schoolClass.getUpdatedAt());
		List<SchoolClassDto.SubjectDto> subjects = fromJson(schoolClass.getSubjectsJson());
		if (subjects.isEmpty()) {
			SchoolClassDto.SubjectDto legacySubject = toLegacySubjectDto(schoolClass);
			if (legacySubject != null) {
				subjects.add(legacySubject);
			}
		}
		dto.setSubjects(subjects);
		return dto;
	}

	private String toJson(List<SchoolClassDto.SubjectDto> subjects) {
		try {
			return objectMapper.writeValueAsString(subjects);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Unable to store subjects data.", e);
		}
	}

	private List<SchoolClassDto.SubjectDto> fromJson(String subjectsJson) {
		if (subjectsJson == null || subjectsJson.isBlank()) {
			return new ArrayList<>();
		}

		try {
			List<SchoolClassDto.SubjectDto> parsed = objectMapper.readValue(subjectsJson, SUBJECT_LIST_TYPE);
			return parsed == null ? new ArrayList<>() : parsed;
		} catch (JsonProcessingException e) {
			// Be tolerant to legacy/corrupted rows so listing classes still works.
			return new ArrayList<>();
		}
	}

	private SchoolClassDto.SubjectDto toLegacySubjectDto(SchoolClass schoolClass) {
		String subjectName = trimToNull(schoolClass.getSubjectName());
		String subjectCode = trimToNull(schoolClass.getSubjectCode());
		if (subjectName == null || subjectCode == null) {
			return null;
		}

		SchoolClassDto.ScheduleSlotDto slot = new SchoolClassDto.ScheduleSlotDto();
		String daysRaw = trimToNull(schoolClass.getScheduleDays());
		List<String> days = new ArrayList<>();
		if (daysRaw != null) {
			days = Arrays.stream(daysRaw.split(","))
					.map(SchoolClassService::trimToNull)
					.filter(day -> day != null)
					.toList();
		}
		slot.setDays(days);
		slot.setStartTime(trimToNull(schoolClass.getStartTime()));
		slot.setEndTime(trimToNull(schoolClass.getEndTime()));

		SchoolClassDto.SubjectDto subject = new SchoolClassDto.SubjectDto();
		subject.setName(subjectName);
		subject.setCode(subjectCode);
		subject.setInstructorId(trimToNull(schoolClass.getProfessorId()));
		subject.setInstructorName(trimToNull(schoolClass.getProfessorId()) != null ? schoolClass.getProfessorId() : "Unknown Instructor");
		subject.setSchedule(List.of(slot));
		return subject;
	}

	private static String requireNonBlank(String value, String message) {
		String trimmed = trimToNull(value);
		if (trimmed == null) {
			throw new IllegalArgumentException(message);
		}
		return trimmed;
	}

	private static void requireMaxLength(String value, int maxLength, String message) {
		if (value != null && value.length() > maxLength) {
			throw new IllegalArgumentException(message);
		}
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static boolean equalsIgnoreCase(String left, String right) {
		if (left == null || right == null) {
			return false;
		}
		return left.equalsIgnoreCase(right);
	}

	private static boolean courseMatches(SchoolClassDto schoolClass, String courseNormalized) {
		if (schoolClass == null || courseNormalized == null) {
			return false;
		}
		return equalsIgnoreCase(trimToNull(schoolClass.getCourseName()), courseNormalized)
				|| equalsIgnoreCase(trimToNull(schoolClass.getCourseCode()), courseNormalized);
	}
}
