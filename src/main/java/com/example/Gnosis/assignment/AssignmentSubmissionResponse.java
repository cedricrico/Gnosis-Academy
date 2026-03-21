package com.example.Gnosis.assignment;

import java.time.Instant;

public class AssignmentSubmissionResponse {
	private Long id;
	private Long assignmentId;
	private String assignmentTitle;
	private String assignmentSubject;
	private String studentId;
	private String studentName;
	private String studentCourse;
	private String studentSection;
	private Integer assignmentPoints;
	private String filename;
	private String downloadUrl;
	private String contentType;
	private Integer grade;
	private String feedback;
	private Instant gradedAt;
	private Instant submittedAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getAssignmentId() {
		return assignmentId;
	}

	public void setAssignmentId(Long assignmentId) {
		this.assignmentId = assignmentId;
	}

	public String getAssignmentTitle() {
		return assignmentTitle;
	}

	public void setAssignmentTitle(String assignmentTitle) {
		this.assignmentTitle = assignmentTitle;
	}

	public String getAssignmentSubject() {
		return assignmentSubject;
	}

	public void setAssignmentSubject(String assignmentSubject) {
		this.assignmentSubject = assignmentSubject;
	}

	public String getStudentId() {
		return studentId;
	}

	public void setStudentId(String studentId) {
		this.studentId = studentId;
	}

	public String getStudentName() {
		return studentName;
	}

	public void setStudentName(String studentName) {
		this.studentName = studentName;
	}

	public String getStudentCourse() {
		return studentCourse;
	}

	public void setStudentCourse(String studentCourse) {
		this.studentCourse = studentCourse;
	}

	public String getStudentSection() {
		return studentSection;
	}

	public void setStudentSection(String studentSection) {
		this.studentSection = studentSection;
	}

	public Integer getAssignmentPoints() {
		return assignmentPoints;
	}

	public void setAssignmentPoints(Integer assignmentPoints) {
		this.assignmentPoints = assignmentPoints;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getDownloadUrl() {
		return downloadUrl;
	}

	public void setDownloadUrl(String downloadUrl) {
		this.downloadUrl = downloadUrl;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public Integer getGrade() {
		return grade;
	}

	public void setGrade(Integer grade) {
		this.grade = grade;
	}

	public String getFeedback() {
		return feedback;
	}

	public void setFeedback(String feedback) {
		this.feedback = feedback;
	}

	public Instant getGradedAt() {
		return gradedAt;
	}

	public void setGradedAt(Instant gradedAt) {
		this.gradedAt = gradedAt;
	}

	public Instant getSubmittedAt() {
		return submittedAt;
	}

	public void setSubmittedAt(Instant submittedAt) {
		this.submittedAt = submittedAt;
	}
}
