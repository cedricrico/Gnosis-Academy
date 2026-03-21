package com.example.Gnosis.web;

import com.example.Gnosis.user.User;
import com.example.Gnosis.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/student/api/profile")
public class StudentProfileApiController {
	private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z .'-]{0,126}$");
	private static final Pattern STRONG_PASSWORD_PATTERN =
			Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");
	private static final Path PROFILE_PHOTO_DIR = Paths.get("uploads", "student-profile");
	private static final String PHOTO_ENDPOINT = "/student/api/profile/photo";

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public StudentProfileApiController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@GetMapping
	public ProfileResponse getProfile(Authentication authentication) {
		return toResponse(resolveStudent(authentication));
	}

	@GetMapping("/photo")
	public ResponseEntity<Resource> getPhoto(Authentication authentication) throws IOException {
		User student = resolveStudent(authentication);
		Path photoPath = resolveProfilePhotoPath(student.getStudentId());
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
		User student = resolveStudent(authentication);
		if (photo == null || photo.isEmpty()) {
			throw new IllegalArgumentException("Photo is required.");
		}

		String extension = resolveExtension(photo);
		Files.createDirectories(PROFILE_PHOTO_DIR);
		clearExistingPhotos(student.getStudentId());
		Path target = PROFILE_PHOTO_DIR.resolve(student.getStudentId() + extension);
		photo.transferTo(target);
		return toResponse(student);
	}

	@PutMapping
	public ProfileResponse updateProfile(
			Authentication authentication,
			@RequestBody ProfileUpdateRequest request
	) {
		User student = resolveStudent(authentication);
		String firstName = requireName(request.firstName(), "First name is required.");
		String lastName = requireName(request.lastName(), "Last name is required.");
		String middleInitial = trimToNull(request.middleInitial());

		if (middleInitial != null) {
			middleInitial = middleInitial.substring(0, 1).toUpperCase(Locale.ROOT);
			if (!middleInitial.matches("[A-Z]")) {
				throw new IllegalArgumentException("Middle initial must be a letter.");
			}
		}

		student.setFirstName(firstName);
		student.setMiddleInitial(middleInitial);
		student.setLastName(lastName);

		return toResponse(userRepository.save(student));
	}

	@PutMapping("/password")
	public void updatePassword(
			Authentication authentication,
			@RequestBody PasswordChangeRequest request
	) {
		User student = resolveStudent(authentication);
		String currentPassword = requireValue(request.currentPassword(), "Current password is required.");
		String newPassword = requireValue(request.newPassword(), "New password is required.");
		String confirmPassword = requireValue(request.confirmPassword(), "Confirm password is required.");

		if (!passwordEncoder.matches(currentPassword, student.getPasswordHash())) {
			throw new IllegalArgumentException("Current password is incorrect.");
		}
		if (!newPassword.equals(confirmPassword)) {
			throw new IllegalArgumentException("Passwords do not match.");
		}
		if (!STRONG_PASSWORD_PATTERN.matcher(newPassword).matches()) {
			throw new IllegalArgumentException(
					"Password must be at least 8 characters and include uppercase, lowercase, and number."
			);
		}
		if (passwordEncoder.matches(newPassword, student.getPasswordHash())) {
			throw new IllegalArgumentException("New password must be different from the current password.");
		}

		student.setPasswordHash(passwordEncoder.encode(newPassword));
		userRepository.save(student);
	}

	@DeleteMapping
	public void deleteProfile(Authentication authentication, HttpServletRequest request) {
		User student = resolveStudent(authentication);
		userRepository.delete(student);
		try {
			clearExistingPhotos(student.getStudentId());
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

	private User resolveStudent(Authentication authentication) {
		if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
			throw new IllegalArgumentException("Authentication required.");
		}
		return userRepository.findByStudentId(authentication.getName())
				.orElseThrow(() -> new NoSuchElementException("Student not found."));
	}

	private static ProfileResponse toResponse(User student) {
		return new ProfileResponse(
				student.getStudentId(),
				student.getFirstName(),
				student.getMiddleInitial(),
				student.getLastName(),
				student.getCourse(),
				student.getSectionName(),
				student.getStatus(),
				resolveProfilePhotoPath(student.getStudentId()) != null ? PHOTO_ENDPOINT : null
		);
	}

	private static Path resolveProfilePhotoPath(String studentId) {
		if (studentId == null || studentId.isBlank()) {
			return null;
		}
		String[] extensions = { ".jpg", ".jpeg", ".png", ".gif", ".webp" };
		for (String ext : extensions) {
			Path candidate = PROFILE_PHOTO_DIR.resolve(studentId + ext);
			if (Files.exists(candidate)) {
				return candidate;
			}
		}
		return null;
	}

	private static void clearExistingPhotos(String studentId) throws IOException {
		if (studentId == null || studentId.isBlank() || !Files.exists(PROFILE_PHOTO_DIR)) {
			return;
		}
		try (var stream = Files.list(PROFILE_PHOTO_DIR)) {
			stream.filter(path -> path.getFileName().toString().startsWith(studentId + "."))
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
				String ext = filename.substring(dot).toLowerCase(Locale.ROOT);
				if (isAllowedExtension(ext)) {
					return ext;
				}
			}
		}

		String contentType = photo.getContentType();
		if (contentType != null) {
			return switch (contentType) {
				case MediaType.IMAGE_PNG_VALUE -> ".png";
				case MediaType.IMAGE_JPEG_VALUE -> ".jpg";
				case MediaType.IMAGE_GIF_VALUE -> ".gif";
				case "image/webp" -> ".webp";
				default -> throw new IllegalArgumentException("Unsupported image type.");
			};
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

	private static String requireName(String value, String message) {
		String trimmed = requireValue(value, message);
		if (!NAME_PATTERN.matcher(trimmed).matches()) {
			throw new IllegalArgumentException("Names may only contain letters, spaces, apostrophes, periods, and hyphens.");
		}
		return trimmed;
	}

	private static String requireValue(String value, String message) {
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

	private record ProfileResponse(
			String studentId,
			String firstName,
			String middleInitial,
			String lastName,
			String course,
			String section,
			String status,
			String photoUrl
		) {
	}

	private record ProfileUpdateRequest(
			String firstName,
			String middleInitial,
			String lastName
	) {
	}

	private record PasswordChangeRequest(
			String currentPassword,
			String newPassword,
			String confirmPassword
	) {
	}
}
