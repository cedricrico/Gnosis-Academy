package com.example.Gnosis.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
	@Order(1)
	@Bean
	public SecurityFilterChain adminSecurityFilterChain(
			HttpSecurity http,
			AdminUserDetailsService adminUserDetailsService,
			PasswordEncoder passwordEncoder
	) throws Exception {
		DaoAuthenticationProvider adminAuthProvider = new DaoAuthenticationProvider(adminUserDetailsService);
		adminAuthProvider.setPasswordEncoder(passwordEncoder);

		http
				.securityMatcher(
						"/admin-login",
						"/admin/login",
						"/admin/dashboard",
						"/admin/logout",
						"/api/admin/**",
						"/admin/**"
				)
				.authenticationProvider(adminAuthProvider)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/admin-login", "/admin/login").permitAll()
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						.requestMatchers("/admin/**").hasRole("ADMIN")
						.anyRequest().authenticated()
				)
				.formLogin(form -> form
						.loginPage("/admin-login")
						.loginProcessingUrl("/admin/login")
						.usernameParameter("username")
						.passwordParameter("password")
						.defaultSuccessUrl("/admin/dashboard", true)
						.failureUrl("/admin-login?error")
						.permitAll()
				)
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint((request, response, authException) -> {
							String uri = request.getRequestURI();
							if (uri != null && uri.startsWith("/api/admin/")) {
								response.sendError(401);
								return;
							}
							response.sendRedirect("/admin-login");
						})
						.accessDeniedHandler((request, response, accessDeniedException) -> {
							String uri = request.getRequestURI();
							if (uri != null && uri.startsWith("/api/admin/")) {
								response.sendError(403);
								return;
							}
							response.sendRedirect("/admin-login?forbidden");
						})
				)
				.logout(logout -> logout
						.logoutUrl("/admin/logout")
						.logoutSuccessUrl("/admin-login?logout")
						.permitAll()
				);

		return http.build();
	}

	@Order(2)
	@Bean
	public SecurityFilterChain professorSecurityFilterChain(
			HttpSecurity http,
			ProfessorIdUserDetailsService professorIdUserDetailsService,
			PasswordEncoder passwordEncoder
	) throws Exception {
		DaoAuthenticationProvider professorAuthProvider = new DaoAuthenticationProvider(professorIdUserDetailsService);
		professorAuthProvider.setPasswordEncoder(passwordEncoder);

		http
				.securityMatcher(
						"/Professor-login",
						"/Professor-registration",
						// Backward-compatible aliases
						"/loginprofessor",
						"/registerprofessor",
						"/professor/**"
				)
				// Logout CSRF failures commonly present as 403/Whitelabel; logging out is safe to exempt.
				.csrf(csrf -> csrf.ignoringRequestMatchers("/professor/logout"))
				.authenticationProvider(professorAuthProvider)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/Professor-login",
								"/Professor-registration",
								// Backward-compatible aliases
								"/loginprofessor",
								"/registerprofessor",
								"/professor/login",
								"/professor/register"
						).permitAll()
						.requestMatchers("/professor/**").hasRole("PROFESSOR")
						.anyRequest().authenticated()
				)
				.formLogin(form -> form
						.loginPage("/Professor-login")
						.loginProcessingUrl("/professor/login")
						.usernameParameter("professorId")
						.passwordParameter("password")
						.defaultSuccessUrl("/professor/home", true)
						.failureUrl("/Professor-login?error")
						.permitAll()
				)
				.logout(logout -> logout
						.logoutUrl("/professor/logout")
						.logoutSuccessUrl("/Professor-login?logout")
						.permitAll()
				);

		return http.build();
	}

	@Order(3)
	@Bean
	public SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			StudentIdUserDetailsService studentIdUserDetailsService,
			PasswordEncoder passwordEncoder
	) throws Exception {
		DaoAuthenticationProvider studentAuthProvider = new DaoAuthenticationProvider(studentIdUserDetailsService);
		studentAuthProvider.setPasswordEncoder(passwordEncoder);

		http
				.authenticationProvider(studentAuthProvider)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/",
								"/landingPage",
								"/Student-login",
								"/Student-registration",
								"/Professor-landing",
								"/professor-landing",

								// Backward-compatible aliases (safe to keep while you update old links).
								"/loginPage",
								"/registrationPage",
								"/register",
								"/error",
								"/js/**",
								"/css/**",
								"/photos/**",
								"/h2-console/**"

						).permitAll()
						.anyRequest().authenticated()
				)
				.formLogin(form -> form
						.loginPage("/Student-login")
						.loginProcessingUrl("/login")
						.usernameParameter("studentId")
						.passwordParameter("password")
						.defaultSuccessUrl("/student/home", true)
						.failureUrl("/Student-login?error")
						.permitAll()
				)
				.logout(logout -> logout
						.logoutUrl("/logout")
						.logoutSuccessUrl("/Student-login?logout")
						.permitAll()
				);

		// H2 console needs these relaxed settings (dev only).
		http.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**", "/logout"));
		http.headers(headers -> headers.frameOptions(frame -> frame.disable()));

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
