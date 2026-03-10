package com.example.Gnosis.admin.dto;

public record StudentDirectoryResponse(
		String studentId,
		String fullName,
		String sex,
		String course,
		String section,
		String status
) {
}
