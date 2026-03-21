package com.example.Gnosis.admin.dto;

public record AdminProfileResponse(
		String username,
		String firstName,
		String lastName,
		String email,
		String role
) {
}
