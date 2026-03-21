package com.example.Gnosis.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminPasswordChangeRequest(
		@NotBlank(message = "Current password is required.")
		String currentPassword,

		@NotBlank(message = "New password is required.")
		@Size(min = 6, message = "Password must be at least 6 characters long.")
		String newPassword,

		@NotBlank(message = "Confirm password is required.")
		String confirmNewPassword
) {
}
