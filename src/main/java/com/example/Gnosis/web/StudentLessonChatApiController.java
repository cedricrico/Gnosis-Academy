package com.example.Gnosis.web;

import com.example.Gnosis.chat.LessonChatRequest;
import com.example.Gnosis.chat.LessonChatResponse;
import com.example.Gnosis.chat.LessonChatContextService;
import com.example.Gnosis.chat.OpenAiLessonChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/student/api/chat")
public class StudentLessonChatApiController {
	private final StudentAccessService studentAccessService;
	private final OpenAiLessonChatService openAiLessonChatService;
	private final LessonChatContextService lessonChatContextService;

	public StudentLessonChatApiController(
			StudentAccessService studentAccessService,
			OpenAiLessonChatService openAiLessonChatService,
			LessonChatContextService lessonChatContextService
	) {
		this.studentAccessService = studentAccessService;
		this.openAiLessonChatService = openAiLessonChatService;
		this.lessonChatContextService = lessonChatContextService;
	}

	@PostMapping("/lessons")
	public ResponseEntity<LessonChatResponse> chat(
			Authentication authentication,
			@RequestBody LessonChatRequest request
	) {
		try {
			studentAccessService.requireStudent(authentication);
			String lessonContext = lessonChatContextService.buildLessonContext(authentication, request);
			return ResponseEntity.ok(openAiLessonChatService.reply(request, lessonContext));
		} catch (NoSuchElementException ex) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(new LessonChatResponse(ex.getMessage()));
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().body(new LessonChatResponse(ex.getMessage()));
		} catch (IllegalStateException ex) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
					.body(new LessonChatResponse(ex.getMessage()));
		}
	}
}
