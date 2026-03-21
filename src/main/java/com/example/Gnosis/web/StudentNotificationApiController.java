package com.example.Gnosis.web;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/student/api/notifications")
public class StudentNotificationApiController {
	private final StudentNotificationService notificationService;

	public StudentNotificationApiController(StudentNotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@GetMapping
	public StudentNotificationService.NotificationPayload list(Authentication authentication) {
		return notificationService.list(authentication);
	}
}
