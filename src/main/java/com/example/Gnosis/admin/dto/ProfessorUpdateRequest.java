package com.example.Gnosis.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfessorUpdateRequest(
		@NotBlank(message = "Employee ID is required.")
		String employeeId,
		@NotBlank(message = "First name is required.")
		@Size(max = 128, message = "First name is too long.")
		String firstName,
		@Size(max = 1, message = "Middle initial must be 1 character.")
		String middleInitial,
		@NotBlank(message = "Last name is required.")
		@Size(max = 128, message = "Last name is too long.")
		String lastName,
		@NotBlank(message = "Department is required.")
		@Size(max = 64, message = "Department is too long.")
		String department,
		@NotBlank(message = "Email is required.")
		@Email(message = "Email must be valid.")
		@Size(max = 128, message = "Email is too long.")
		String email
) {
}
