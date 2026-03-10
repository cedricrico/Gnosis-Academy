package com.example.Gnosis.admin.service;

import com.example.Gnosis.schoolclass.SchoolClassDto;
import com.example.Gnosis.schoolclass.SchoolClassService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AdminClassService {
	private final SchoolClassService schoolClassService;

	public AdminClassService(SchoolClassService schoolClassService) {
		this.schoolClassService = schoolClassService;
	}

	public List<SchoolClassDto> listClasses() {
		return schoolClassService.findAll();
	}

	public Optional<SchoolClassDto> getClassById(Long classId) {
		return schoolClassService.findById(classId);
	}

	public SchoolClassDto createClass(SchoolClassDto payload) {
		return schoolClassService.create(payload);
	}

	public SchoolClassDto updateClass(Long classId, SchoolClassDto payload) {
		return schoolClassService.update(classId, payload);
	}

	public void deleteClass(Long classId) {
		schoolClassService.delete(classId);
	}
}
