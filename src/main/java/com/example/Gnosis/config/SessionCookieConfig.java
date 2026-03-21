package com.example.Gnosis.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
public class SessionCookieConfig {
	private static final String ADMIN_COOKIE_NAME = "ADMIN_SESSION";
	private static final String PROFESSOR_COOKIE_NAME = "PROFESSOR_SESSION";

	@Bean
	public CookieSerializer cookieSerializer() {
		DefaultCookieSerializer defaultSerializer = new DefaultCookieSerializer();

		DefaultCookieSerializer adminSerializer = new DefaultCookieSerializer();
		adminSerializer.setCookieName(ADMIN_COOKIE_NAME);

		DefaultCookieSerializer professorSerializer = new DefaultCookieSerializer();
		professorSerializer.setCookieName(PROFESSOR_COOKIE_NAME);

		return new CookieSerializer() {
			@Override
			public java.util.List<String> readCookieValues(HttpServletRequest request) {
				if (isAdminRequest(request)) {
					return adminSerializer.readCookieValues(request);
				}
				if (isProfessorRequest(request)) {
					return professorSerializer.readCookieValues(request);
				}
				return defaultSerializer.readCookieValues(request);
			}

			@Override
			public void writeCookieValue(CookieValue cookieValue) {
				if (isAdminRequest(cookieValue.getRequest())) {
					adminSerializer.writeCookieValue(cookieValue);
				} else if (isProfessorRequest(cookieValue.getRequest())) {
					professorSerializer.writeCookieValue(cookieValue);
				} else {
					defaultSerializer.writeCookieValue(cookieValue);
				}
			}

			private boolean isAdminRequest(HttpServletRequest request) {
				if (request == null) {
					return false;
				}
				String uri = request.getRequestURI();
				if (uri == null) {
					return false;
				}
				return uri.startsWith("/admin")
						|| uri.startsWith("/api/admin");
			}

			private boolean isProfessorRequest(HttpServletRequest request) {
				if (request == null) {
					return false;
				}
				String uri = request.getRequestURI();
				if (uri == null) {
					return false;
				}
				return uri.startsWith("/professor")
						|| uri.startsWith("/Professor-login")
						|| uri.startsWith("/Professor-registration")
						|| uri.startsWith("/loginprofessor")
						|| uri.startsWith("/registerprofessor");
			}
		};
	}
}
