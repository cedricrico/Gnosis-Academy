package com.example.Gnosis.admin.api;

import com.example.Gnosis.admin.dto.AdminPasswordChangeRequest;
import com.example.Gnosis.admin.dto.AdminProfileResponse;
import com.example.Gnosis.admin.dto.AdminProfileUpdateRequest;
import com.example.Gnosis.admin.service.AdminProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/profile")
public class AdminProfileController {
	private final AdminProfileService adminProfileService;

	public AdminProfileController(AdminProfileService adminProfileService) {
		this.adminProfileService = adminProfileService;
	}

	@GetMapping
	public AdminProfileResponse getProfile(Authentication authentication) {
		return adminProfileService.getProfile(authentication.getName());
	}

	@PutMapping
	public AdminProfileResponse updateProfile(
			Authentication authentication,
			@Valid @RequestBody AdminProfileUpdateRequest payload
	) {
		return adminProfileService.updateProfile(authentication.getName(), payload);
	}

	@PutMapping("/password")
	public ResponseEntity<Void> changePassword(
			Authentication authentication,
			@Valid @RequestBody AdminPasswordChangeRequest payload
	) {
		adminProfileService.changePassword(authentication.getName(), payload);
		return ResponseEntity.noContent().build();
	}
}
