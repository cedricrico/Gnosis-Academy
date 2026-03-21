package com.example.Gnosis.assignment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
		name = "assignment_submissions",
		uniqueConstraints = @UniqueConstraint(name = "uk_assignment_submission_student", columnNames = {"assignmentId", "studentId"})
)
public class AssignmentSubmission {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long assignmentId;

	@Column(nullable = false, length = 200)
	private String assignmentTitle;

	@Column(length = 200)
	private String assignmentSubject;

	@Column(nullable = false, length = 32)
	private String professorId;

	@Column(nullable = false, length = 32)
	private String studentId;

	@Column(nullable = false, length = 128)
	private String studentName;

	@Column(length = 128)
	private String studentCourse;

	@Column(length = 128)
	private String studentSection;

	@Column(nullable = false, length = 255)
	private String filename;

	@Column(nullable = false, length = 512)
	private String filePath;

	@Column(length = 128)
	private String contentType;

	private Long fileSize;

	@Column(nullable = false)
	private Instant submittedAt;

	protected AssignmentSubmission() {
	}

	public Long getId() {
		return id;
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

	public String getProfessorId() {
		return professorId;
	}

	public void setProfessorId(String professorId) {
		this.professorId = professorId;
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

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public Long getFileSize() {
		return fileSize;
	}

	public void setFileSize(Long fileSize) {
		this.fileSize = fileSize;
	}

	public Instant getSubmittedAt() {
		return submittedAt;
	}

	@PrePersist
	void onCreate() {
		submittedAt = Instant.now();
	}
}
