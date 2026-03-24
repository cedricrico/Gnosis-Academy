package com.example.Gnosis.chat;

public record LessonChatRequest(
		String message,
		LessonChatContext context
) {
	public record LessonChatContext(
			String page,
			String selectedSubject,
			String url
	) {
	}
}
