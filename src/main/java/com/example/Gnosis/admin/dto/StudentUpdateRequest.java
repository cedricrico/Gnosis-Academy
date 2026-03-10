package com.example.Gnosis.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StudentUpdateRequest(
		@NotBlank(message = "Full name is required.")
		@Size(max = 256, message = "Full name is too long.")
		String fullName,
		@Size(max = 128, message = "Course is too long.")
		String course,
		@Size(max = 128, message = "Section is too long.")
		String section,
		@Pattern(regexp = "Active|Dropped", message = "Status must be Active or Dropped.")
		String status
) {
}
