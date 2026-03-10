package com.example.Gnosis.security;

import com.example.Gnosis.professor.Professor;
import com.example.Gnosis.professor.ProfessorRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ProfessorIdUserDetailsService implements UserDetailsService {
	private final ProfessorRepository professorRepository;

	public ProfessorIdUserDetailsService(ProfessorRepository professorRepository) {
		this.professorRepository = professorRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String employeeId) throws UsernameNotFoundException {
		Professor professor = professorRepository.findByEmployeeId(employeeId)
				.orElseThrow(() -> new UsernameNotFoundException("Professor not found"));

		return new ProfessorUserDetails(
				professor.getEmployeeId(),
				professor.getPasswordHash(),
				professor.getFirstName(),
				professor.getLastName()
		);
	}
}

