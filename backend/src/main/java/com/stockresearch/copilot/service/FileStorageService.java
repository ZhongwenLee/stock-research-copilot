package com.stockresearch.copilot.service;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface FileStorageService {

	StoredFile store(Long companyId, MultipartFile file);

	Path resolve(String storagePath);

	record StoredFile(String storagePath, String originalFileName, String fileExt, long fileSize) {
	}
}
