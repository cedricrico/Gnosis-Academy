package com.example.Gnosis.activity;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ProfessorActivityService {
	private final ProfessorActivityRepository professorActivityRepository;

	public ProfessorActivityService(ProfessorActivityRepository professorActivityRepository) {
		this.professorActivityRepository = professorActivityRepository;
	}

	@Transactional
	public void record(
			String professorId,
			String action,
			String entityType,
			String entityId,
			String title,
			String details
	) {
		if (professorId == null || professorId.isBlank()) {
			return;
		}
		ProfessorActivity activity = new ProfessorActivity(
				professorId,
				action,
				entityType,
				entityId,
				title,
				details
		);
		professorActivityRepository.save(activity);
	}

	@Transactional(readOnly = true)
	public List<ProfessorActivity> listRecent(String professorId, int limit) {
		List<ProfessorActivity> all = professorActivityRepository.findByProfessorIdOrderByCreatedAtDesc(professorId);
		if (limit <= 0 || all.size() <= limit) {
			return all;
		}
		return all.subList(0, limit);
	}

	@Transactional
	public void delete(Long activityId, String professorId) {
		ProfessorActivity activity = professorActivityRepository.findById(activityId)
				.orElseThrow(() -> new NoSuchElementException("Activity not found."));
		if (!activity.getProfessorId().equals(professorId)) {
			throw new IllegalArgumentException("Unauthorized activity delete.");
		}
		professorActivityRepository.delete(activity);
	}
}
