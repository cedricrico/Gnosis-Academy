package com.example.Gnosis.quiz;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long quizId;

	@Column(nullable = false, length = 32)
	private String studentId;

	@Column(nullable = false, length = 255)
	private String studentName;

	@Column(length = 128)
	private String course;

	@Column(length = 128)
	private String section;

	@Column(name = "attempt_number", nullable = false)
	private Integer attemptsCount;

	@Column(name = "total_questions", nullable = false)
	private Integer totalItems;

	@Column(name = "correct_answers", nullable = false)
	private Integer score;

	@Column(nullable = false, columnDefinition = "LONGTEXT")
	private String answersJson;

	@Column(nullable = false)
	private Instant submittedAt;

	protected QuizAttempt() {
	}

	public Long getId() {
		return id;
	}

	public Long getQuizId() {
		return quizId;
	}

	public void setQuizId(Long quizId) {
		this.quizId = quizId;
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

	public String getCourse() {
		return course;
	}

	public void setCourse(String course) {
		this.course = course;
	}

	public String getSection() {
		return section;
	}

	public void setSection(String section) {
		this.section = section;
	}

	public Integer getAttemptsCount() {
		return attemptsCount;
	}

	public void setAttemptsCount(Integer attemptsCount) {
		this.attemptsCount = attemptsCount;
	}

	public Integer getTotalItems() {
		return totalItems;
	}

	public void setTotalItems(Integer totalItems) {
		this.totalItems = totalItems;
	}

	public Integer getScore() {
		return score;
	}

	public void setScore(Integer score) {
		this.score = score;
	}

	public String getAnswersJson() {
		return answersJson;
	}

	public void setAnswersJson(String answersJson) {
		this.answersJson = answersJson;
	}

	public Instant getSubmittedAt() {
		return submittedAt;
	}

	public Integer getAttemptNumber() {
		return attemptsCount;
	}

	public void setAttemptNumber(Integer attemptNumber) {
		this.attemptsCount = attemptNumber;
	}

	public Integer getTotalQuestions() {
		return totalItems;
	}

	public void setTotalQuestions(Integer totalQuestions) {
		this.totalItems = totalQuestions;
	}

	public Integer getCorrectAnswers() {
		return score;
	}

	public void setCorrectAnswers(Integer correctAnswers) {
		this.score = correctAnswers;
	}

	public Instant getDateTaken() {
		return submittedAt;
	}

	@PrePersist
	void onCreate() {
		submittedAt = Instant.now();
	}
}
