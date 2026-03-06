package com.example.Gnosis.schoolclass;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SchoolClassDto {
	private Long id;
	private String courseCode;
	private String courseName;
	private String sectionName;
	private List<SubjectDto> subjects = new ArrayList<>();
	private Instant createdAt;
	private Instant updatedAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCourseCode() {
		return courseCode;
	}

	public void setCourseCode(String courseCode) {
		this.courseCode = courseCode;
	}

	public String getCourseName() {
		return courseName;
	}

	public void setCourseName(String courseName) {
		this.courseName = courseName;
	}

	public String getSectionName() {
		return sectionName;
	}

	public void setSectionName(String sectionName) {
		this.sectionName = sectionName;
	}

	public List<SubjectDto> getSubjects() {
		return subjects;
	}

	public void setSubjects(List<SubjectDto> subjects) {
		this.subjects = subjects == null ? new ArrayList<>() : subjects;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	public static class SubjectDto {
		private String name;
		private String code;
		private String instructorId;
		private String instructorName;
		private List<ScheduleSlotDto> schedule = new ArrayList<>();

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public String getInstructorId() {
			return instructorId;
		}

		public void setInstructorId(String instructorId) {
			this.instructorId = instructorId;
		}

		public String getInstructorName() {
			return instructorName;
		}

		public void setInstructorName(String instructorName) {
			this.instructorName = instructorName;
		}

		public List<ScheduleSlotDto> getSchedule() {
			return schedule;
		}

		public void setSchedule(List<ScheduleSlotDto> schedule) {
			this.schedule = schedule == null ? new ArrayList<>() : schedule;
		}
	}

	public static class ScheduleSlotDto {
		private List<String> days = new ArrayList<>();
		private String startTime;
		private String endTime;

		public List<String> getDays() {
			return days;
		}

		public void setDays(List<String> days) {
			this.days = days == null ? new ArrayList<>() : days;
		}

		public String getStartTime() {
			return startTime;
		}

		public void setStartTime(String startTime) {
			this.startTime = startTime;
		}

		public String getEndTime() {
			return endTime;
		}

		public void setEndTime(String endTime) {
			this.endTime = endTime;
		}
	}
}
