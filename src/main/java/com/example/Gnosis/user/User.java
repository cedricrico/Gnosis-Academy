package com.example.Gnosis.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "users")
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 32)
	private String studentId;

	@Column(nullable = false, length = 128)
	private String firstName;

	@Column(length = 1)
	private String middleInitial;

	@Column(nullable = false, length = 128)
	private String lastName;

	@Column(nullable = false)
	private Integer age;

	@Column(nullable = false, length = 16)
	private String sex;

	@Column(nullable = false, length = 100)
	private String passwordHash;

	@Column(nullable = false)
	private Instant createdAt = Instant.now();

	protected User() {
	}

	public User(
			String studentId,
			String firstName,
			String middleInitial,
			String lastName,
			Integer age,
			String sex,
			String passwordHash
	) {
		this.studentId = studentId;
		this.firstName = firstName;
		this.middleInitial = middleInitial;
		this.lastName = lastName;
		this.age = age;
		this.sex = sex;
		this.passwordHash = passwordHash;
	}

	public Long getId() {
		return id;
	}

	public String getStudentId() {
		return studentId;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getMiddleInitial() {
		return middleInitial;
	}

	public String getLastName() {
		return lastName;
	}

	public Integer getAge() {
		return age;
	}

	public String getSex() {
		return sex;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}

