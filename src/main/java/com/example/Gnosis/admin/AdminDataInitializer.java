package com.example.Gnosis.admin;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminDataInitializer implements CommandLineRunner {
	private static final String DEFAULT_ADMIN_USERNAME = "admin";
	private static final String DEFAULT_ADMIN_PASSWORD = "admin123";
	private static final String DEFAULT_ADMIN_FIRST_NAME = "Admin";
	private static final String DEFAULT_ADMIN_LAST_NAME = "User";
	private static final String DEFAULT_ADMIN_EMAIL = "admin@school.edu";

	private final AdminRepository adminRepository;
	private final PasswordEncoder passwordEncoder;

	public AdminDataInitializer(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
		this.adminRepository = adminRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public void run(String... args) {
		Admin admin = adminRepository.findByUsername(DEFAULT_ADMIN_USERNAME)
				.orElseGet(() -> adminRepository.save(
						new Admin(
								DEFAULT_ADMIN_USERNAME,
								DEFAULT_ADMIN_FIRST_NAME,
								DEFAULT_ADMIN_LAST_NAME,
								DEFAULT_ADMIN_EMAIL,
								passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD)
						)
				));

		if (!isBcryptHash(admin.getPassword())) {
			admin.setPassword(passwordEncoder.encode(admin.getPassword()));
		}
		if (isBlank(admin.getFirstName())) {
			admin.setFirstName(DEFAULT_ADMIN_FIRST_NAME);
		}
		if (isBlank(admin.getLastName())) {
			admin.setLastName(DEFAULT_ADMIN_LAST_NAME);
		}
		if (isBlank(admin.getEmail())) {
			admin.setEmail(DEFAULT_ADMIN_EMAIL);
		}
		adminRepository.save(admin);
	}

	private static boolean isBcryptHash(String value) {
		return value != null && value.matches("^\\$2[aby]\\$\\d\\d\\$.{53}$");
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
