package com.example.Gnosis.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminProfileUpdateRequest(
		@NotBlank(message = "First name is required.")
		@Size(max = 128, message = "First name must be 128 characters or fewer.")
		String firstName,

		@NotBlank(message = "Last name is required.")
		@Size(max = 128, message = "Last name must be 128 characters or fewer.")
		String lastName,

		@NotBlank(message = "Email is required.")
		@Email(message = "Email must be valid.")
		@Size(max = 128, message = "Email must be 128 characters or fewer.")
		String email
) {
}
