package com.example.Gnosis.quiz;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
	long countByQuizIdAndStudentId(Long quizId, String studentId);
	long countByQuizId(Long quizId);
	List<QuizAttempt> findByQuizIdInOrderByStudentIdAscQuizIdAscAttemptsCountDesc(List<Long> quizIds);
	List<QuizAttempt> findByQuizIdOrderByStudentIdAscAttemptsCountDesc(Long quizId);
	List<QuizAttempt> findByQuizIdAndStudentIdOrderByAttemptsCountDesc(Long quizId, String studentId);
	java.util.Optional<QuizAttempt> findByQuizIdAndStudentIdAndAttemptsCount(Long quizId, String studentId, Integer attemptsCount);
	java.util.Optional<QuizAttempt> findFirstByQuizIdAndStudentIdOrderByAttemptsCountDesc(Long quizId, String studentId);

	@Query(value = """
			select qa.attempt_number as attemptNumber,
			       qa.correct_answers as correctAnswers,
			       qa.total_questions as totalQuestions
			from quiz_attempts qa
			where qa.quiz_id = :quizId
			  and qa.student_id = :studentId
			order by qa.attempt_number desc
			limit 1
			""", nativeQuery = true)
	java.util.Optional<LatestAttemptSummaryProjection> findLatestAttemptSummary(
			@Param("quizId") Long quizId,
			@Param("studentId") String studentId
	);

	@Query(value = """
			select qa.id as id,
			       qa.attempt_number as attemptNumber,
			       qa.correct_answers as correctAnswers,
			       qa.total_questions as totalQuestions,
			       qa.answers_json as answersJson
			from quiz_attempts qa
			where qa.quiz_id = :quizId
			  and qa.student_id = :studentId
			  and qa.attempt_number = :attemptNumber
			limit 1
			""", nativeQuery = true)
	java.util.Optional<AttemptResultProjection> findAttemptResult(
			@Param("quizId") Long quizId,
			@Param("studentId") String studentId,
			@Param("attemptNumber") Integer attemptNumber
	);

	interface LatestAttemptSummaryProjection {
		Integer getAttemptNumber();
		Integer getCorrectAnswers();
		Integer getTotalQuestions();
	}

	interface AttemptResultProjection {
		Long getId();
		Integer getAttemptNumber();
		Integer getCorrectAnswers();
		Integer getTotalQuestions();
		String getAnswersJson();
	}
}
