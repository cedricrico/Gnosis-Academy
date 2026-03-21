package com.example.Gnosis.quiz;

import java.time.Instant;

public class QuizResponse {
	private Long id;
	private String title;
	private String code;
	private String subject;
	private String section;
	private Integer durationMinutes;
	private Integer questionCount;
	private Integer attempts;
	private String status;
	private String dueDate;
	private String description;
	private String questionsJson;
	private String professorId;
	private String professorName;
	private Instant createdAt;
	private Instant updatedAt;
	private Integer attemptsUsed;
	private Integer attemptsRemaining;
	private Boolean canAttempt;
	private Integer latestScore;
	private Integer latestScoreTotal;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getSection() {
		return section;
	}

	public void setSection(String section) {
		this.section = section;
	}

	public Integer getDurationMinutes() {
		return durationMinutes;
	}

	public void setDurationMinutes(Integer durationMinutes) {
		this.durationMinutes = durationMinutes;
	}

	public Integer getQuestionCount() {
		return questionCount;
	}

	public void setQuestionCount(Integer questionCount) {
		this.questionCount = questionCount;
	}

	public Integer getAttempts() {
		return attempts;
	}

	public void setAttempts(Integer attempts) {
		this.attempts = attempts;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getDueDate() {
		return dueDate;
	}

	public void setDueDate(String dueDate) {
		this.dueDate = dueDate;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getQuestionsJson() {
		return questionsJson;
	}

	public void setQuestionsJson(String questionsJson) {
		this.questionsJson = questionsJson;
	}

	public String getProfessorId() {
		return professorId;
	}

	public void setProfessorId(String professorId) {
		this.professorId = professorId;
	}

	public String getProfessorName() {
		return professorName;
	}

	public void setProfessorName(String professorName) {
		this.professorName = professorName;
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

	public Integer getAttemptsUsed() {
		return attemptsUsed;
	}

	public void setAttemptsUsed(Integer attemptsUsed) {
		this.attemptsUsed = attemptsUsed;
	}

	public Integer getAttemptsRemaining() {
		return attemptsRemaining;
	}

	public void setAttemptsRemaining(Integer attemptsRemaining) {
		this.attemptsRemaining = attemptsRemaining;
	}

	public Boolean getCanAttempt() {
		return canAttempt;
	}

	public void setCanAttempt(Boolean canAttempt) {
		this.canAttempt = canAttempt;
	}

	public Integer getLatestScore() {
		return latestScore;
	}

	public void setLatestScore(Integer latestScore) {
		this.latestScore = latestScore;
	}

	public Integer getLatestScoreTotal() {
		return latestScoreTotal;
	}

	public void setLatestScoreTotal(Integer latestScoreTotal) {
		this.latestScoreTotal = latestScoreTotal;
	}
}
