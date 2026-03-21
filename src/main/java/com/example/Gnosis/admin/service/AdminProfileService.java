package com.example.Gnosis.admin.service;

import com.example.Gnosis.admin.Admin;
import com.example.Gnosis.admin.AdminRepository;
import com.example.Gnosis.admin.dto.AdminPasswordChangeRequest;
import com.example.Gnosis.admin.dto.AdminProfileResponse;
import com.example.Gnosis.admin.dto.AdminProfileUpdateRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class AdminProfileService {
	private final AdminRepository adminRepository;
	private final PasswordEncoder passwordEncoder;

	public AdminProfileService(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
		this.adminRepository = adminRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public AdminProfileResponse getProfile(String username) {
		return toResponse(loadAdmin(username));
	}

	public AdminProfileResponse updateProfile(String username, AdminProfileUpdateRequest payload) {
		Admin admin = loadAdmin(username);
		admin.setFirstName(payload.firstName().trim());
		admin.setLastName(payload.lastName().trim());
		admin.setEmail(payload.email().trim());
		return toResponse(adminRepository.save(admin));
	}

	public void changePassword(String username, AdminPasswordChangeRequest payload) {
		Admin admin = loadAdmin(username);
		if (!passwordEncoder.matches(payload.currentPassword(), admin.getPassword())) {
			throw new IllegalArgumentException("Current password is incorrect.");
		}
		if (!payload.newPassword().equals(payload.confirmNewPassword())) {
			throw new IllegalArgumentException("New passwords do not match.");
		}
		admin.setPassword(passwordEncoder.encode(payload.newPassword()));
		adminRepository.save(admin);
	}

	private Admin loadAdmin(String username) {
		return adminRepository.findByUsername(username)
				.orElseThrow(() -> new NoSuchElementException("Admin not found: " + username));
	}

	private static AdminProfileResponse toResponse(Admin admin) {
		return new AdminProfileResponse(
				admin.getUsername(),
				admin.getFirstName(),
				admin.getLastName(),
				admin.getEmail(),
				"Administrator"
		);
	}
}
