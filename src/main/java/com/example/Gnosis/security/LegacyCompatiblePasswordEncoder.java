package com.example.Gnosis.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class LegacyCompatiblePasswordEncoder implements PasswordEncoder {
	private final BCryptPasswordEncoder delegate = new BCryptPasswordEncoder();

	@Override
	public String encode(CharSequence rawPassword) {
		return delegate.encode(rawPassword);
	}

	@Override
	public boolean matches(CharSequence rawPassword, String storedPassword) {
		if (storedPassword == null) {
			return false;
		}
		if (isBcryptHash(storedPassword)) {
			return delegate.matches(rawPassword, storedPassword);
		}
		return storedPassword.contentEquals(rawPassword);
	}

	@Override
	public boolean upgradeEncoding(String encodedPassword) {
		return !isBcryptHash(encodedPassword);
	}

	private static boolean isBcryptHash(String value) {
		return value != null && value.matches("^\\$2[aby]\\$\\d\\d\\$.{53}$");
	}
}
