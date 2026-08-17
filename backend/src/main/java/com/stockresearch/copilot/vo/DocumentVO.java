package com.stockresearch.copilot.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class DocumentVO {

	private Long id;
	private Long companyId;
	private String title;
	private String docType;
	private String fileName;
	private String fileExt;
	private Long fileSize;
	private LocalDate publishDate;
	private String processStatus;
	private String errorMessage;
	private String storagePath;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
