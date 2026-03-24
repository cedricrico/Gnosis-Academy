package com.example.Gnosis.chat;

import com.example.Gnosis.lesson.Lesson;
import com.example.Gnosis.lesson.LessonService;
import com.example.Gnosis.schoolclass.SchoolClassDto;
import com.example.Gnosis.schoolclass.SchoolClassService;
import com.example.Gnosis.web.StudentAccessService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
public class LessonChatContextService {
	private static final int MAX_LESSONS = 6;
	private static final int MAX_DESCRIPTION_CHARS = 500;
	private static final int MAX_ATTACHMENT_CHARS = 4000;
	private static final Pattern XML_TAGS = Pattern.compile("<[^>]+>");
	private static final Pattern XML_ESCAPES = Pattern.compile("&(?:amp|lt|gt|quot|apos);");
	private static final Pattern SLIDE_ENTRY = Pattern.compile("ppt/slides/slide(\\d+)\\.xml");

	private final LessonService lessonService;
	private final SchoolClassService schoolClassService;
	private final StudentAccessService studentAccessService;

	public LessonChatContextService(
			LessonService lessonService,
			SchoolClassService schoolClassService,
			StudentAccessService studentAccessService
	) {
		this.lessonService = lessonService;
		this.schoolClassService = schoolClassService;
		this.studentAccessService = studentAccessService;
	}

	public String buildLessonContext(Authentication authentication, LessonChatRequest request) {
		StudentAccessService.StudentAccessContext accessContext = studentAccessService.resolve(authentication);
		if (!accessContext.canAccessClassContent()) {
			return "No accessible lessons were found for this student.";
		}

		Set<String> allowedSubjects = resolveAllowedSubjects(accessContext);
		List<Lesson> accessibleLessons = lessonService.listEntitiesForStudentSection(accessContext.section(), allowedSubjects);
		if (accessibleLessons.isEmpty()) {
			return "No uploaded lessons are currently available for this student's section and subjects.";
		}

		String selectedSubject = request != null && request.context() != null ? normalizeValue(request.context().selectedSubject()) : null;
		List<Lesson> prioritized = prioritize(accessibleLessons, selectedSubject);
		StringBuilder context = new StringBuilder();
		context.append("Accessible uploaded lessons.\n");
		context.append("Treat each lesson below as a separate, self-contained source.\n");
		context.append("Do not combine details from different lessons unless the student explicitly asks to compare them.\n");
		int count = 0;
		for (Lesson lesson : prioritized) {
			if (count >= MAX_LESSONS) {
				break;
			}
			appendLesson(context, lesson, count + 1);
			count++;
		}
		if (accessibleLessons.size() > count) {
			context.append("\nAdditional lessons are available but omitted for brevity.\n");
		}
		return context.toString().trim();
	}

	private static List<Lesson> prioritize(List<Lesson> lessons, String selectedSubject) {
		List<Lesson> copy = new ArrayList<>(lessons);
		copy.sort(Comparator
				.comparing((Lesson lesson) -> !matchesSubject(lesson.getSubject(), selectedSubject))
				.thenComparing(Lesson::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
				.thenComparing(Lesson::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
		return copy;
	}

	private void appendLesson(StringBuilder context, Lesson lesson, int lessonNumber) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);
		context.append("\n=== LESSON ").append(lessonNumber).append(" START ===\n");
		context.append("Lesson ID: ").append(lesson.getId() != null ? lesson.getId() : "Unknown").append("\n");
		context.append("Lesson: ").append(defaultValue(lesson.getTitle(), "Untitled")).append("\n");
		context.append("Subject: ").append(defaultValue(lesson.getSubject(), "Unknown")).append("\n");
		context.append("Week: ").append(defaultValue(lesson.getWeek(), "Unknown")).append("\n");
		context.append("Type: ").append(defaultValue(lesson.getType(), "Unknown")).append("\n");
		context.append("Section: ").append(defaultValue(lesson.getSection(), "Unknown")).append("\n");
		context.append("Professor: ").append(defaultValue(lesson.getProfessorName(), "Unknown")).append("\n");
		if (lesson.getUpdatedAt() != null) {
			context.append("Updated: ")
					.append(formatter.format(lesson.getUpdatedAt().atZone(ZoneId.systemDefault()).toLocalDate()))
					.append("\n");
		}
		context.append("Description:\n").append(limit(lesson.getDescription(), MAX_DESCRIPTION_CHARS)).append("\n");
		String attachmentText = extractAttachmentText(lesson);
		if (attachmentText != null) {
			context.append("Attachment: ").append(defaultValue(lesson.getAttachmentName(), "Unknown")).append("\n");
			context.append("Attachment excerpt:\n").append(limit(attachmentText, MAX_ATTACHMENT_CHARS)).append("\n");
		}
		context.append("=== LESSON ").append(lessonNumber).append(" END ===\n");
	}

	private String extractAttachmentText(Lesson lesson) {
		String pathValue = normalizeValue(lesson.getAttachmentPath());
		if (pathValue == null) {
			return null;
		}
		Path path = Path.of(pathValue);
		if (!Files.isRegularFile(path)) {
			return null;
		}
		String filename = defaultValue(lesson.getAttachmentName(), path.getFileName().toString()).toLowerCase(Locale.ENGLISH);
		try {
			if (filename.endsWith(".pdf")) {
				return extractPdfText(path);
			}
			if (filename.endsWith(".docx")) {
				return extractDocxText(path);
			}
			if (filename.endsWith(".pptx")) {
				return extractPptxText(path);
			}
		} catch (Exception ignored) {
			return null;
		}
		return null;
	}

	private static String extractPdfText(Path path) throws IOException {
		AutoCloseable document = null;
		try {
			document = loadPdfDocument(path);
			Class<?> documentClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
			Class<?> stripperClass = Class.forName("org.apache.pdfbox.text.PDFTextStripper");
			Method getNumberOfPages = documentClass.getMethod("getNumberOfPages");
			int totalPages = (Integer) getNumberOfPages.invoke(document);
			Method setStartPage = stripperClass.getMethod("setStartPage", int.class);
			Method setEndPage = stripperClass.getMethod("setEndPage", int.class);
			Method getText = stripperClass.getMethod("getText", documentClass);
			StringBuilder text = new StringBuilder();
			for (int page = 1; page <= totalPages; page++) {
				Object stripper = stripperClass.getConstructor().newInstance();
				setStartPage.invoke(stripper, page);
				setEndPage.invoke(stripper, page);
				String pageText = normalizeExtractedText((String) getText.invoke(stripper, document));
				if (pageText == null) {
					continue;
				}
				if (!text.isEmpty()) {
					text.append("\n");
				}
				text.append("[Page ").append(page).append("] ").append(pageText);
			}
			return normalizeValue(text.toString());
		} catch (ReflectiveOperationException ex) {
			return null;
		} finally {
			if (document != null) {
				try {
					document.close();
				} catch (Exception ignored) {
				}
			}
		}
	}

	private static AutoCloseable loadPdfDocument(Path path) throws ReflectiveOperationException {
		try {
			Class<?> loaderClass = Class.forName("org.apache.pdfbox.Loader");
			Method loadPdf = loaderClass.getMethod("loadPDF", java.io.File.class);
			return (AutoCloseable) loadPdf.invoke(null, path.toFile());
		} catch (ClassNotFoundException ex) {
			Class<?> documentClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
			Method load = documentClass.getMethod("load", java.io.File.class);
			return (AutoCloseable) load.invoke(null, path.toFile());
		}
	}

	private static String extractDocxText(Path path) throws IOException {
		return extractZipXmlText(path, List.of("word/document.xml"));
	}

	private static String extractPptxText(Path path) throws IOException {
		List<String> entries = new ArrayList<>();
		try (ZipFile zipFile = new ZipFile(path.toFile())) {
			zipFile.stream()
					.map(ZipEntry::getName)
					.filter(name -> name.startsWith("ppt/slides/slide") && name.endsWith(".xml"))
					.sorted()
					.forEach(entries::add);
		}
		if (entries.isEmpty()) {
			return null;
		}
		StringBuilder text = new StringBuilder();
		try (ZipFile zipFile = new ZipFile(path.toFile())) {
			for (String entryName : entries) {
				ZipEntry entry = zipFile.getEntry(entryName);
				if (entry == null) {
					continue;
				}
				try (InputStream inputStream = zipFile.getInputStream(entry)) {
					String xml = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
					String cleaned = normalizeExtractedText(decodeXmlEscapes(XML_TAGS.matcher(xml).replaceAll(" ")));
					if (cleaned == null) {
						continue;
					}
					Matcher matcher = SLIDE_ENTRY.matcher(entryName);
					String label = matcher.matches() ? "[Slide " + matcher.group(1) + "] " : "";
					if (!text.isEmpty()) {
						text.append("\n");
					}
					text.append(label).append(cleaned);
				}
			}
		}
		return normalizeValue(text.toString());
	}

	private static String extractZipXmlText(Path path, List<String> entryNames) throws IOException {
		if (entryNames.isEmpty()) {
			return null;
		}
		StringBuilder text = new StringBuilder();
		try (ZipFile zipFile = new ZipFile(path.toFile())) {
			for (String entryName : entryNames) {
				ZipEntry entry = zipFile.getEntry(entryName);
				if (entry == null) {
					continue;
				}
				try (InputStream inputStream = zipFile.getInputStream(entry)) {
					String xml = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
					String cleaned = normalizeExtractedText(decodeXmlEscapes(XML_TAGS.matcher(xml).replaceAll(" ")));
					if (cleaned != null) {
						if (!text.isEmpty()) {
							text.append("\n");
						}
						text.append(cleaned);
					}
				}
			}
		}
		return normalizeExtractedText(text.toString());
	}

	private static String decodeXmlEscapes(String value) {
		if (value == null) {
			return null;
		}
		String decoded = value
				.replace("&amp;", "&")
				.replace("&lt;", "<")
				.replace("&gt;", ">")
				.replace("&quot;", "\"")
				.replace("&apos;", "'");
		return XML_ESCAPES.matcher(decoded).replaceAll(" ");
	}

	private static String normalizeExtractedText(String value) {
		String normalized = normalizeValue(value);
		if (normalized == null) {
			return null;
		}
		return normalized.replaceAll("\\s+", " ").trim();
	}

	private Set<String> resolveAllowedSubjects(StudentAccessService.StudentAccessContext context) {
		if (context.course() == null || context.section() == null) {
			return Set.of();
		}
		List<SchoolClassDto> classes = schoolClassService.findForStudent(context.course(), context.section());
		LinkedHashSet<String> subjects = new LinkedHashSet<>();
		for (SchoolClassDto schoolClass : classes) {
			for (SchoolClassDto.SubjectDto subject : schoolClass.getSubjects()) {
				String code = normalizeValue(subject.getCode());
				String name = normalizeValue(subject.getName());
				if (code != null && name != null) {
					subjects.add(code + " - " + name);
				}
				if (code != null) {
					subjects.add(code);
				}
				if (name != null) {
					subjects.add(name);
				}
			}
		}
		return subjects;
	}

	private static boolean matchesSubject(String lessonSubject, String selectedSubject) {
		if (selectedSubject == null) {
			return true;
		}
		String lesson = normalizeValue(lessonSubject);
		return lesson != null && lesson.equalsIgnoreCase(selectedSubject);
	}

	private static String limit(String value, int maxChars) {
		String normalized = normalizeValue(value);
		if (normalized == null) {
			return "";
		}
		if (normalized.length() <= maxChars) {
			return normalized;
		}
		return normalized.substring(0, maxChars) + "...";
	}

	private static String normalizeValue(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static String defaultValue(String value, String fallback) {
		String normalized = normalizeValue(value);
		return normalized != null ? normalized : fallback;
	}
}
