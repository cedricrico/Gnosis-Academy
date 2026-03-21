package com.example.Gnosis.admin.dto;

public record ProfessorDirectoryResponse(
		Long id,
		String employeeId,
		String fullName,
		String department,
		String email
) {
}
