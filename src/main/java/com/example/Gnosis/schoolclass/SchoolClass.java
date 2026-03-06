package com.example.Gnosis.schoolclass;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "school_classes")
public class SchoolClass {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 32)
	private String courseCode;

	@Column(nullable = false, length = 128)
	private String courseName;

	// Legacy column kept non-null in existing MySQL schema.
	@Column(name = "course", nullable = false, length = 128)
	private String course;

	@Column(nullable = false, length = 128)
	private String sectionName;

	// Legacy columns kept non-null in existing MySQL schema.
	@Column(name = "subject_name", nullable = false, length = 128)
	private String subjectName;

	@Column(name = "subject_code", nullable = false, length = 64)
	private String subjectCode;

	@Column(name = "professor_id", nullable = false, length = 32)
	private String professorId;

	@Column(name = "schedule_days", nullable = false, length = 64)
	private String scheduleDays;

	@Column(name = "start_time", nullable = false, length = 16)
	private String startTime;

	@Column(name = "end_time", nullable = false, length = 16)
	private String endTime;

	@Column(name = "status", nullable = false, length = 32)
	private String status;

	@Lob
	@Column(nullable = false)
	private String subjectsJson;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	protected SchoolClass() {
	}

	public Long getId() {
		return id;
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

	public String getCourse() {
		return course;
	}

	public void setCourse(String course) {
		this.course = course;
	}

	public String getSectionName() {
		return sectionName;
	}

	public void setSectionName(String sectionName) {
		this.sectionName = sectionName;
	}

	public String getSubjectName() {
		return subjectName;
	}

	public void setSubjectName(String subjectName) {
		this.subjectName = subjectName;
	}

	public String getSubjectCode() {
		return subjectCode;
	}

	public void setSubjectCode(String subjectCode) {
		this.subjectCode = subjectCode;
	}

	public String getProfessorId() {
		return professorId;
	}

	public void setProfessorId(String professorId) {
		this.professorId = professorId;
	}

	public String getScheduleDays() {
		return scheduleDays;
	}

	public void setScheduleDays(String scheduleDays) {
		this.scheduleDays = scheduleDays;
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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getSubjectsJson() {
		return subjectsJson;
	}

	public void setSubjectsJson(String subjectsJson) {
		this.subjectsJson = subjectsJson;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}
}
