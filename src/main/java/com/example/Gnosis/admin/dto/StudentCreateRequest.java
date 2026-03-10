package com.example.Gnosis.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StudentCreateRequest(
		@NotBlank(message = "Student ID is required.")
		@Pattern(regexp = "^\\d{4}-\\d{5}$", message = "Student ID must match 0000-00000.")
		String studentId,
		@NotBlank(message = "Full name is required.")
		@Size(max = 256, message = "Full name is too long.")
		String fullName,
		@NotBlank(message = "Course is required.")
		@Size(max = 128, message = "Course is too long.")
		String course,
		@NotBlank(message = "Section is required.")
		@Size(max = 128, message = "Section is too long.")
		String section,
		@NotBlank(message = "Status is required.")
		@Pattern(regexp = "Active|Dropped", message = "Status must be Active or Dropped.")
		String status,
		@NotBlank(message = "Password is required.")
		@Size(min = 8, message = "Password must be at least 8 characters.")
		String password
) {
}
