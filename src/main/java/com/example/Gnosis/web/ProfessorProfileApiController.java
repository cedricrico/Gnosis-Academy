package com.example.Gnosis.web;

import com.example.Gnosis.professor.Professor;
import com.example.Gnosis.professor.ProfessorRepository;
import com.example.Gnosis.security.ProfessorUserDetails;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/professor/api/profile")
public class ProfessorProfileApiController {
	private static final Path PROFILE_PHOTO_DIR = Paths.get("uploads", "professor-profile");
	private static final String PHOTO_ENDPOINT = "/professor/api/profile/photo";
	private final ProfessorRepository professorRepository;
	private final PasswordEncoder passwordEncoder;

	public ProfessorProfileApiController(ProfessorRepository professorRepository, PasswordEncoder passwordEncoder) {
		this.professorRepository = professorRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@GetMapping
	public ProfileResponse getProfile(Authentication authentication) {
		Professor professor = resolveProfessor(authentication);
		return toResponse(professor);
	}

	@GetMapping("/photo")
	public ResponseEntity<Resource> getPhoto(Authentication authentication) throws IOException {
		Professor professor = resolveProfessor(authentication);
		Path photoPath = resolveProfilePhotoPath(professor.getEmployeeId());
		if (photoPath == null) {
			return ResponseEntity.notFound().build();
		}

		Resource resource = new UrlResource(photoPath.toUri());
		String contentType = Files.probeContentType(photoPath);
		if (contentType == null || contentType.isBlank()) {
			contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
		}
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(contentType))
				.body(resource);
	}

	@PutMapping("/photo")
	public ProfileResponse updatePhoto(
			Authentication authentication,
			@RequestParam("photo") MultipartFile photo
	) throws IOException {
		Professor professor = resolveProfessor(authentication);
		if (photo == null || photo.isEmpty()) {
			throw new IllegalArgumentException("Photo is required.");
		}

		String extension = resolveExtension(photo);
		Files.createDirectories(PROFILE_PHOTO_DIR);
		clearExistingPhotos(professor.getEmployeeId());
		Path target = PROFILE_PHOTO_DIR.resolve(professor.getEmployeeId() + extension);
		photo.transferTo(target);

		return toResponse(professor);
	}

	@PutMapping
	public ProfileResponse updateProfile(
			Authentication authentication,
			@RequestBody ProfileUpdateRequest request
	) {
		Professor professor = resolveProfessor(authentication);
		String firstName = requireValue(request.firstName(), "First name is required.");
		String lastName = requireValue(request.lastName(), "Last name is required.");
		String department = requireValue(request.department(), "Department is required.");
		String email = requireValue(request.email(), "Email is required.");
		String middleInitial = trimToNull(request.middleInitial());
		if (middleInitial != null && middleInitial.length() > 1) {
			middleInitial = middleInitial.substring(0, 1);
		}

		if (!email.equalsIgnoreCase(professor.getEmail())
				&& professorRepository.findByEmail(email).isPresent()) {
			throw new IllegalArgumentException("Email already exists.");
		}

		professor.setFirstName(firstName);
		professor.setMiddleInitial(middleInitial);
		professor.setLastName(lastName);
		professor.setDepartment(department);
		professor.setEmail(email);

		Professor saved = professorRepository.save(professor);
		return toResponse(saved);
	}

	@PutMapping("/password")
	public void updatePassword(
			Authentication authentication,
			@RequestBody PasswordChangeRequest request
	) {
		Professor professor = resolveProfessor(authentication);
		String currentPassword = requireValue(request.currentPassword(), "Current password is required.");
		String newPassword = requireValue(request.newPassword(), "New password is required.");
		String confirmPassword = requireValue(request.confirmPassword(), "Confirm password is required.");

		if (!passwordEncoder.matches(currentPassword, professor.getPasswordHash())) {
			throw new IllegalArgumentException("Current password is incorrect.");
		}
		if (!newPassword.equals(confirmPassword)) {
			throw new IllegalArgumentException("Passwords do not match.");
		}
		if (newPassword.length() < 8) {
			throw new IllegalArgumentException("Password must be at least 8 characters.");
		}

		professor.setPasswordHash(passwordEncoder.encode(newPassword));
		professorRepository.save(professor);
	}

	@DeleteMapping
	public void deleteProfile(Authentication authentication, HttpServletRequest request) {
		Professor professor = resolveProfessor(authentication);
		professorRepository.delete(professor);
		try {
			clearExistingPhotos(professor.getEmployeeId());
		} catch (IOException ignored) {
		}

		if (request != null) {
			var session = request.getSession(false);
			if (session != null) {
				session.invalidate();
			}
		}
		SecurityContextHolder.clearContext();
	}

	private Professor resolveProfessor(Authentication authentication) {
		if (authentication == null) {
			throw new IllegalArgumentException("Authentication required.");
		}
		String employeeId = authentication.getName();
		if (authentication.getPrincipal() instanceof ProfessorUserDetails pud) {
			employeeId = pud.getEmployeeId();
		}
		return professorRepository.findByEmployeeId(employeeId)
				.orElseThrow(() -> new NoSuchElementException("Professor not found."));
	}

	private static ProfileResponse toResponse(Professor professor) {
		String photoUrl = resolveProfilePhotoPath(professor.getEmployeeId()) != null
				? PHOTO_ENDPOINT
				: null;
		return new ProfileResponse(
				professor.getEmployeeId(),
				professor.getFirstName(),
				professor.getMiddleInitial(),
				professor.getLastName(),
				professor.getDepartment(),
				professor.getEmail(),
				photoUrl
		);
	}

	private static Path resolveProfilePhotoPath(String employeeId) {
		if (employeeId == null || employeeId.isBlank()) {
			return null;
		}
		String[] extensions = { ".jpg", ".jpeg", ".png", ".gif", ".webp" };
		for (String ext : extensions) {
			Path candidate = PROFILE_PHOTO_DIR.resolve(employeeId + ext);
			if (Files.exists(candidate)) {
				return candidate;
			}
		}
		return null;
	}

	private static void clearExistingPhotos(String employeeId) throws IOException {
		if (employeeId == null || employeeId.isBlank()) {
			return;
		}
		if (!Files.exists(PROFILE_PHOTO_DIR)) {
			return;
		}
		try (var stream = Files.list(PROFILE_PHOTO_DIR)) {
			stream.filter(path -> path.getFileName().toString().startsWith(employeeId + "."))
					.forEach(path -> {
						try {
							Files.deleteIfExists(path);
						} catch (IOException ignored) {
						}
					});
		}
	}

	private static String resolveExtension(MultipartFile photo) {
		String filename = photo.getOriginalFilename();
		if (filename != null) {
			int dot = filename.lastIndexOf('.');
			if (dot >= 0 && dot < filename.length() - 1) {
				String ext = filename.substring(dot).toLowerCase();
				if (isAllowedExtension(ext)) {
					return ext;
				}
			}
		}

		String contentType = photo.getContentType();
		if (contentType != null) {
			String mapped = switch (contentType) {
				case MediaType.IMAGE_PNG_VALUE -> ".png";
				case MediaType.IMAGE_JPEG_VALUE -> ".jpg";
				case MediaType.IMAGE_GIF_VALUE -> ".gif";
				case "image/webp" -> ".webp";
				default -> null;
			};
			if (mapped != null) {
				return mapped;
			}
		}

		throw new IllegalArgumentException("Unsupported image type.");
	}

	private static boolean isAllowedExtension(String ext) {
		return ".jpg".equals(ext)
				|| ".jpeg".equals(ext)
				|| ".png".equals(ext)
				|| ".gif".equals(ext)
				|| ".webp".equals(ext);
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static String requireValue(String value, String message) {
		String trimmed = trimToNull(value);
		if (trimmed == null) {
			throw new IllegalArgumentException(message);
		}
		return trimmed;
	}

	private record ProfileResponse(
			String employeeId,
			String firstName,
			String middleInitial,
			String lastName,
			String department,
			String email,
			String photoUrl
	) {}

	private record ProfileUpdateRequest(
			String firstName,
			String middleInitial,
			String lastName,
			String department,
			String email
	) {}

	private record PasswordChangeRequest(
			String currentPassword,
			String newPassword,
			String confirmPassword
	) {}
}
