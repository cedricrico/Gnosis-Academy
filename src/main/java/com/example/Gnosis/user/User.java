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

	@Column(length = 128)
	private String course;

	@Column(name = "section_name", length = 128)
	private String sectionName;

	@Column(length = 32)
	private String status;

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

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getMiddleInitial() {
		return middleInitial;
	}

	public void setMiddleInitial(String middleInitial) {
		this.middleInitial = middleInitial;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	public String getSex() {
		return sex;
	}

	public void setSex(String sex) {
		this.sex = sex;
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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}

