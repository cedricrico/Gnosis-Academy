package com.example.Gnosis.chat;

import com.example.Gnosis.config.ChatProperties;
import com.example.Gnosis.config.OpenAiProperties;
import com.example.Gnosis.config.OllamaProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Service
public class OpenAiLessonChatService {
	private static final Logger log = LoggerFactory.getLogger(OpenAiLessonChatService.class);

	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final ChatProperties chatProperties;
	private final OpenAiProperties openAiProperties;
	private final OllamaProperties ollamaProperties;

	public OpenAiLessonChatService(
			RestClient restClient,
			ObjectMapper objectMapper,
			ChatProperties chatProperties,
			OpenAiProperties openAiProperties,
			OllamaProperties ollamaProperties
	) {
		this.restClient = restClient;
		this.objectMapper = objectMapper;
		this.chatProperties = chatProperties;
		this.openAiProperties = openAiProperties;
		this.ollamaProperties = ollamaProperties;
	}

	public LessonChatResponse reply(LessonChatRequest request) {
		return reply(request, null);
	}

	public LessonChatResponse reply(LessonChatRequest request, String lessonContext) {
		String provider = defaultValue(chatProperties.provider(), "ollama").toLowerCase();
		if ("ollama".equals(provider)) {
			return replyWithOllama(request, lessonContext);
		}
		if ("openai".equals(provider)) {
			return replyWithOpenAi(request, lessonContext);
		}
		if ("auto".equals(provider)) {
			String apiKey = trimToNull(openAiProperties.apiKey());
			return apiKey != null ? replyWithOpenAi(request, lessonContext) : replyWithOllama(request, lessonContext);
		}
		return fallbackResponse("Unsupported chat provider: " + provider + ". Use openai, ollama, or auto.");
	}

	private LessonChatResponse replyWithOpenAi(LessonChatRequest request, String lessonContext) {
		String apiKey = trimToNull(openAiProperties.apiKey());
		if (apiKey == null) {
			return fallbackResponse("Lesson Assistant is not configured yet. Add an OpenAI API key to enable live answers.");
		}

		String userMessage = trimToNull(request.message());
		if (userMessage == null) {
			throw new IllegalArgumentException("Message is required.");
		}

		Map<String, Object> payload = Map.of(
				"model", defaultValue(openAiProperties.model(), "gpt-4.1-mini"),
				"input", List.of(
						Map.of(
								"role", "system",
								"content", List.of(Map.of("type", "input_text", "text", buildSystemPrompt(request.context(), lessonContext)))
						),
						Map.of(
								"role", "user",
								"content", List.of(Map.of("type", "input_text", "text", userMessage))
						)
				)
		);

		try {
			String responseBody = restClient.post()
					.uri(defaultValue(openAiProperties.responsesUrl(), "https://api.openai.com/v1/responses"))
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
					.contentType(MediaType.APPLICATION_JSON)
					.accept(MediaType.APPLICATION_JSON)
					.body(payload)
					.retrieve()
					.body(String.class);
			JsonNode response = readJson(responseBody, "OpenAI");

			String message = extractAssistantText(response);
			if (message == null) {
				throw new IllegalStateException("OpenAI response did not contain message text.");
			}
			return new LessonChatResponse(message);
		} catch (RestClientResponseException ex) {
			log.warn("OpenAI request failed with status {} and body: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
			if (ex.getStatusCode().value() == 429) {
				return fallbackResponse("Lesson Assistant is temporarily unavailable because the OpenAI API quota has been reached. Please try again later or ask the administrator to enable billing.");
			}
			if (ex.getStatusCode().value() == 401) {
				return fallbackResponse("Lesson Assistant is unavailable because the OpenAI API key is invalid or expired. Please contact the administrator.");
			}
			throw new IllegalStateException(buildHttpErrorMessage(ex), ex);
		} catch (RestClientException ex) {
			log.warn("OpenAI request failed before receiving a valid response.", ex);
			return fallbackResponse("Lesson Assistant is temporarily unavailable. Please try again later.");
		}
	}

	private LessonChatResponse replyWithOllama(LessonChatRequest request, String lessonContext) {
		String userMessage = trimToNull(request.message());
		if (userMessage == null) {
			throw new IllegalArgumentException("Message is required.");
		}

		String baseUrl = defaultValue(ollamaProperties.baseUrl(), "http://localhost:11434");
		String chatUrl = defaultValue(ollamaProperties.chatUrl(), "/api/chat");
		String model = defaultValue(ollamaProperties.model(), "llama3.2");

		Map<String, Object> payload = Map.of(
				"model", model,
				"stream", false,
				"messages", List.of(
						Map.of("role", "system", "content", buildSystemPrompt(request.context(), lessonContext)),
						Map.of("role", "user", "content", userMessage)
				)
		);

		try {
			String responseBody = restClient.post()
					.uri(baseUrl + chatUrl)
					.contentType(MediaType.APPLICATION_JSON)
					.accept(MediaType.APPLICATION_JSON)
					.body(payload)
					.retrieve()
					.body(String.class);
			JsonNode response = readJson(responseBody, "Ollama");

			String message = trimToNull(response.path("message").path("content").asText(null));
			if (message == null) {
				throw new IllegalStateException("Ollama response did not contain message text.");
			}
			return new LessonChatResponse(message);
		} catch (RestClientResponseException ex) {
			log.warn("Ollama request failed with status {} and body: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
			throw new IllegalStateException(buildProviderErrorMessage("Ollama", ex), ex);
		} catch (RestClientException ex) {
			log.warn("Ollama request failed before receiving a valid response.", ex);
			return fallbackResponse("Lesson Assistant could not reach Ollama. Start Ollama locally and pull a model such as llama3.2.");
		}
	}

	private static String buildSystemPrompt(LessonChatRequest.LessonChatContext context, String lessonContext) {
		String page = context != null ? trimToNull(context.page()) : null;
		String selectedSubject = context != null ? trimToNull(context.selectedSubject()) : null;
		String url = context != null ? trimToNull(context.url()) : null;

		StringBuilder prompt = new StringBuilder();
		prompt.append("You are the Gnosis Academy Lesson Coach. ");
		prompt.append("Your job is active recall coaching only, not answer-giving. ");
		prompt.append("Each uploaded lesson is independent unless the student explicitly asks to compare lessons. ");
		prompt.append("Never assume all uploaded lessons are interconnected, sequential, or about the same concept. ");
		prompt.append("You must ask the student short lesson-based questions, evaluate the student's response, and coach them toward the correct source material. ");
		prompt.append("Never reveal the final answer directly, even if the student asks for it. ");
		prompt.append("If the student is wrong, incomplete, or unsure, respond with encouragement plus a hint and tell them exactly which uploaded file and page/slide number to review when that reference exists in the lesson context. ");
		prompt.append("If a page/slide reference is not available, point them to the lesson title and attachment name only. ");
		prompt.append("Ask one question at a time. Keep responses concise and student-friendly. ");
		prompt.append("If the student has not answered yet and instead asks for help, turn it into a coaching question or a small hint. ");
		prompt.append("Do not provide model answers, answer keys, full solutions, or direct final explanations. ");
		prompt.append("If the lesson context does not contain enough information, say that and ask the student to open the relevant lesson file.");
		if (page != null || selectedSubject != null || url != null) {
			prompt.append(" Current page context:");
			if (page != null) {
				prompt.append(" page=").append(page).append(";");
			}
			if (selectedSubject != null) {
				prompt.append(" subject=").append(selectedSubject).append(";");
			}
			if (url != null) {
				prompt.append(" url=").append(url).append(";");
			}
		}
		String lessons = trimToNull(lessonContext);
		if (lessons != null) {
			prompt.append(" Use the following uploaded lesson context as your only course source of truth. Prefer it over general knowledge:\n");
			prompt.append(lessons);
		}
		prompt.append("\nCoaching rules:");
		prompt.append("\n1. Start by asking or continuing one recall question grounded in the uploaded lessons.");
		prompt.append("\n2. Choose one most relevant lesson block first. Stay inside that lesson block unless the student explicitly asks to connect or compare lessons.");
		prompt.append("\n3. If the student's answer is incorrect, do not reveal the answer. Give a brief hint and cite the lesson attachment plus page/slide reference if available.");
		prompt.append("\n4. If the student's answer is partly correct, acknowledge what is correct and ask a follow-up question.");
		prompt.append("\n5. If the student asks for the answer directly, refuse briefly and redirect them to the relevant file/page/slide, then ask another guiding question.");
		prompt.append("\n6. When citing references, use this format: Review `<attachment name>` page X or slide Y.");
		prompt.append("\n7. Do not merge terminology, facts, or examples from another lesson block unless the student explicitly asks for comparison.");
		return prompt.toString();
	}

	private static String extractAssistantText(JsonNode response) {
		if (response == null) {
			return null;
		}

		JsonNode output = response.path("output");
		if (!output.isArray()) {
			return null;
		}

		StringBuilder text = new StringBuilder();
		for (JsonNode item : output) {
			JsonNode content = item.path("content");
			if (!content.isArray()) {
				continue;
			}
			for (JsonNode part : content) {
				String value = trimToNull(part.path("text").asText(null));
				if (value != null) {
					if (!text.isEmpty()) {
						text.append("\n\n");
					}
					text.append(value);
				}
			}
		}

		return trimToNull(text.toString());
	}

	private static String defaultValue(String value, String fallback) {
		String trimmed = trimToNull(value);
		return trimmed != null ? trimmed : fallback;
	}

	private JsonNode readJson(String responseBody, String provider) {
		try {
			return objectMapper.readTree(defaultValue(responseBody, "{}"));
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException(provider + " returned invalid JSON.", ex);
		}
	}

	private static String buildHttpErrorMessage(RestClientResponseException ex) {
		String body = trimToNull(ex.getResponseBodyAsString());
		if (body != null && body.length() > 240) {
			body = body.substring(0, 240) + "...";
		}
		if (body == null) {
			return "OpenAI request failed with status " + ex.getStatusCode() + ".";
		}
		return "OpenAI request failed with status " + ex.getStatusCode() + ": " + body;
	}

	private static String buildProviderErrorMessage(String provider, RestClientResponseException ex) {
		String body = trimToNull(ex.getResponseBodyAsString());
		if (body != null && body.length() > 240) {
			body = body.substring(0, 240) + "...";
		}
		if (body == null) {
			return provider + " request failed with status " + ex.getStatusCode() + ".";
		}
		return provider + " request failed with status " + ex.getStatusCode() + ": " + body;
	}

	private static LessonChatResponse fallbackResponse(String message) {
		return new LessonChatResponse(message);
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
