package com.example.Gnosis.auth;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
	@NotBlank
	@Size(min = 2, max = 128)
	@Pattern(regexp = "^[A-Za-z]+([ '\\-][A-Za-z]+)*$")
	private String firstName;

	@Size(max = 1)
	@Pattern(regexp = "^[A-Za-z]?$")
	private String middleInitial;

	@NotBlank
	@Size(min = 2, max = 128)
	@Pattern(regexp = "^[A-Za-z]+([ '\\-][A-Za-z]+)*$")
	private String lastName;

	@NotNull
	@Min(1)
	@Max(130)
	private Integer age;

	@NotBlank
	@Pattern(regexp = "^(male|female|other)$")
	private String sex;

	@NotBlank
	@Pattern(regexp = "^[0-9]{4}-[0-9]{5}$")
	private String studentId;

	@NotBlank
	@Size(min = 8, max = 72)
	private String password;

	@NotBlank
	private String confirmPassword;

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

	public String getStudentId() {
		return studentId;
	}

	public void setStudentId(String studentId) {
		this.studentId = studentId;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getConfirmPassword() {
		return confirmPassword;
	}

	public void setConfirmPassword(String confirmPassword) {
		this.confirmPassword = confirmPassword;
	}
}
