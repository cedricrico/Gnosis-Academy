package com.example.Gnosis.security;

import com.example.Gnosis.admin.Admin;
import com.example.Gnosis.admin.AdminRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminUserDetailsService implements UserDetailsService {
	private final AdminRepository adminRepository;

	public AdminUserDetailsService(AdminRepository adminRepository) {
		this.adminRepository = adminRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Admin admin = adminRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("Admin not found"));

		return org.springframework.security.core.userdetails.User
				.withUsername(admin.getUsername())
				.password(admin.getPassword())
				.authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
				.build();
	}
}
