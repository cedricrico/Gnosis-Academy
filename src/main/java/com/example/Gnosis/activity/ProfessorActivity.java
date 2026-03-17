package com.example.Gnosis.activity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "professor_activity")
public class ProfessorActivity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 32)
	private String professorId;

	@Column(nullable = false, length = 32)
	private String action;

	@Column(nullable = false, length = 32)
	private String entityType;

	@Column(length = 64)
	private String entityId;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(length = 500)
	private String details;

	@Column(nullable = false)
	private Instant createdAt;

	protected ProfessorActivity() {
	}

	public ProfessorActivity(
			String professorId,
			String action,
			String entityType,
			String entityId,
			String title,
			String details
	) {
		this.professorId = professorId;
		this.action = action;
		this.entityType = entityType;
		this.entityId = entityId;
		this.title = title;
		this.details = details;
	}

	public Long getId() {
		return id;
	}

	public String getProfessorId() {
		return professorId;
	}

	public String getAction() {
		return action;
	}

	public String getEntityType() {
		return entityType;
	}

	public String getEntityId() {
		return entityId;
	}

	public String getTitle() {
		return title;
	}

	public String getDetails() {
		return details;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}
}
