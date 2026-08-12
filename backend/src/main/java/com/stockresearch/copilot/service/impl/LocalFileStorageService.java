package com.stockresearch.copilot.service.impl;

import com.stockresearch.copilot.common.exception.BizException;
import com.stockresearch.copilot.common.exception.ErrorCode;
import com.stockresearch.copilot.config.AppProperties;
import com.stockresearch.copilot.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {

	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy/MM/dd");

	private final AppProperties appProperties;

	@Override
	public StoredFile store(Long companyId, MultipartFile file) {
		validate(file);
		String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "unnamed" : file.getOriginalFilename());
		if (originalName.contains("..")) {
			throw new BizException(ErrorCode.VALIDATION_FAILED, "invalid file name");
		}
		String ext = extractExt(originalName);
		assertAllowed(ext);

		Path root = Paths.get(appProperties.getFileStorage().getPath()).toAbsolutePath().normalize();
		Path dir = root.resolve(String.valueOf(companyId)).resolve(LocalDate.now().format(DAY));
		try {
			Files.createDirectories(dir);
			String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
			Path target = dir.resolve(storedName);
			file.transferTo(target);
			String relative = root.relativize(target).toString().replace('\\', '/');
			log.info("stored upload companyId={} path={} size={}", companyId, relative, file.getSize());
			return new StoredFile(relative, originalName, ext, file.getSize());
		}
		catch (IOException ex) {
			throw new BizException(ErrorCode.INTERNAL_ERROR, "failed to store file: " + ex.getMessage());
		}
	}

	@Override
	public Path resolve(String storagePath) {
		Path root = Paths.get(appProperties.getFileStorage().getPath()).toAbsolutePath().normalize();
		Path resolved = root.resolve(storagePath).normalize();
		if (!resolved.startsWith(root)) {
			throw new BizException(ErrorCode.BAD_REQUEST, "invalid storage path");
		}
		return resolved;
	}

	private void validate(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BizException(ErrorCode.VALIDATION_FAILED, "file is required");
		}
		if (file.getSize() > appProperties.getFileStorage().getMaxFileSizeBytes()) {
			throw new BizException(ErrorCode.VALIDATION_FAILED, "file exceeds max size");
		}
		String name = file.getOriginalFilename();
		if (name == null || name.isBlank() || name.length() > 255) {
			throw new BizException(ErrorCode.VALIDATION_FAILED, "invalid file name");
		}
	}

	private void assertAllowed(String ext) {
		Set<String> allowed = appProperties.getFileStorage().getAllowedExtensions().stream()
				.map(item -> item.toLowerCase(Locale.ROOT))
				.collect(Collectors.toSet());
		if (!allowed.contains(ext)) {
			throw new BizException(ErrorCode.VALIDATION_FAILED,
					"unsupported file type: " + ext + ", allowed=" + allowed);
		}
	}

	private String extractExt(String fileName) {
		int idx = fileName.lastIndexOf('.');
		if (idx < 0 || idx == fileName.length() - 1) {
			throw new BizException(ErrorCode.VALIDATION_FAILED, "file extension is required");
		}
		return fileName.substring(idx + 1).toLowerCase(Locale.ROOT);
	}
}
