package com.example.Gnosis.security;

import com.example.Gnosis.user.User;
import com.example.Gnosis.user.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentIdUserDetailsService implements UserDetailsService {
	private final UserRepository userRepository;

	public StudentIdUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String studentId) throws UsernameNotFoundException {
		User user = userRepository.findByStudentId(studentId)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));

		return org.springframework.security.core.userdetails.User
				.withUsername(user.getStudentId())
				.password(user.getPasswordHash())
				.authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
				.build();
	}
}

