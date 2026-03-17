package com.example.Gnosis.assignment;

public class AssignmentRequest {
	private String title;
	private String description;
	private String subject;
	private java.util.List<String> sections;
	private String status;
	private String dueDate;
	private Integer points;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public java.util.List<String> getSections() {
		return sections;
	}

	public void setSections(java.util.List<String> sections) {
		this.sections = sections;
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

	public Integer getPoints() {
		return points;
	}

	public void setPoints(Integer points) {
		this.points = points;
	}
}
