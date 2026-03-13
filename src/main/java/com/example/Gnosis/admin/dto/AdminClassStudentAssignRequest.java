package com.example.Gnosis.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AdminClassStudentAssignRequest(
		@NotBlank
		@Pattern(regexp = "^\\d{4}-\\d{5}$", message = "Student ID must match 0000-00000.")
		String studentId
) {
}

