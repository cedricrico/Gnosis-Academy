package com.example.Gnosis.quiz;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Gnosis.activity.ProfessorActivityService;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class QuizService {
	private final QuizRepository quizRepository;
	private final ProfessorActivityService activityService;

	public QuizService(QuizRepository quizRepository, ProfessorActivityService activityService) {
		this.quizRepository = quizRepository;
		this.activityService = activityService;
	}

	@Transactional(readOnly = true)
	public List<QuizResponse> listForProfessor(String professorId) {
		return quizRepository.findByProfessorIdOrderByCreatedAtDesc(professorId)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	public QuizResponse create(String professorId, String professorName, QuizRequest request) {
		Quiz quiz = new Quiz();
		applyRequest(quiz, professorId, professorName, request);
		Quiz saved = quizRepository.save(quiz);
		activityService.record(professorId, "CREATED", "QUIZ", String.valueOf(saved.getId()),
				saved.getTitle(), "Created quiz.");
		return toResponse(saved);
	}

	@Transactional
	public QuizResponse update(Long id, String professorId, String professorName, QuizRequest request) {
		Quiz quiz = quizRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Quiz not found."));
		if (!quiz.getProfessorId().equals(professorId)) {
			throw new IllegalArgumentException("Unauthorized quiz update.");
		}
		applyRequest(quiz, professorId, professorName, request);
		Quiz saved = quizRepository.save(quiz);
		activityService.record(professorId, "UPDATED", "QUIZ", String.valueOf(saved.getId()),
				saved.getTitle(), "Updated quiz.");
		return toResponse(saved);
	}

	@Transactional
	public void delete(Long id, String professorId) {
		Quiz quiz = quizRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Quiz not found."));
		if (!quiz.getProfessorId().equals(professorId)) {
			throw new IllegalArgumentException("Unauthorized quiz delete.");
		}
		String title = quiz.getTitle();
		quizRepository.delete(quiz);
		activityService.record(professorId, "DELETED", "QUIZ", String.valueOf(id),
				title != null ? title : "Quiz", "Deleted quiz.");
	}

	private void applyRequest(Quiz quiz, String professorId, String professorName, QuizRequest request) {
		String title = requireText(request.getTitle(), "Title is required.");
		String code = requireText(request.getCode(), "Code is required.");
		String subject = requireText(request.getSubject(), "Subject is required.");
		String status = requireText(request.getStatus(), "Status is required.");
		Integer durationMinutes = request.getDurationMinutes();
		Integer questionCount = request.getQuestionCount();
		if (durationMinutes == null || durationMinutes < 1) {
			throw new IllegalArgumentException("Duration must be at least 1 minute.");
		}
		if (questionCount == null || questionCount < 1) {
			throw new IllegalArgumentException("At least one question is required.");
		}

		quiz.setTitle(title);
		quiz.setCode(code);
		quiz.setSubject(subject);
		quiz.setSection(trimToNull(request.getSection()));
		quiz.setDurationMinutes(durationMinutes);
		quiz.setQuestionCount(questionCount);
		quiz.setAttempts(request.getAttempts());
		quiz.setDescription(trimToNull(request.getDescription()));
		quiz.setDueDate(trimToNull(request.getDueDate()));
		String questionsJson = trimToNull(request.getQuestionsJson());
		if (questionsJson == null) {
			questionsJson = quiz.getQuestionsJson();
		}
		if (questionsJson == null) {
			questionsJson = "[]";
		}
		quiz.setQuestionsJson(questionsJson);
		quiz.setStatus(status.toUpperCase());
		quiz.setProfessorId(professorId);
		quiz.setProfessorName(professorName);
	}

	private QuizResponse toResponse(Quiz quiz) {
		QuizResponse response = new QuizResponse();
		response.setId(quiz.getId());
		response.setTitle(quiz.getTitle());
		response.setCode(quiz.getCode());
		response.setSubject(quiz.getSubject());
		response.setSection(quiz.getSection());
		response.setDurationMinutes(quiz.getDurationMinutes());
		response.setQuestionCount(quiz.getQuestionCount());
		response.setAttempts(quiz.getAttempts());
		response.setStatus(quiz.getStatus());
		response.setDueDate(quiz.getDueDate());
		response.setDescription(quiz.getDescription());
		response.setQuestionsJson(quiz.getQuestionsJson());
		response.setProfessorId(quiz.getProfessorId());
		response.setProfessorName(quiz.getProfessorName());
		response.setCreatedAt(quiz.getCreatedAt());
		response.setUpdatedAt(quiz.getUpdatedAt());
		return response;
	}

	private static String requireText(String value, String message) {
		String trimmed = trimToNull(value);
		if (trimmed == null) {
			throw new IllegalArgumentException(message);
		}
		return trimmed;
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
