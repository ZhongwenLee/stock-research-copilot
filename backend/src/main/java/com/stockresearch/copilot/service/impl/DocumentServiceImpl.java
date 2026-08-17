package com.stockresearch.copilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stockresearch.copilot.common.enums.DocType;
import com.stockresearch.copilot.common.enums.ProcessStatus;
import com.stockresearch.copilot.common.enums.SourceType;
import com.stockresearch.copilot.common.exception.BizException;
import com.stockresearch.copilot.common.exception.ErrorCode;
import com.stockresearch.copilot.common.result.PageResult;
import com.stockresearch.copilot.config.AppProperties;
import com.stockresearch.copilot.dto.DocumentQueryRequest;
import com.stockresearch.copilot.entity.Company;
import com.stockresearch.copilot.entity.Document;
import com.stockresearch.copilot.entity.DocumentSource;
import com.stockresearch.copilot.mapper.CompanyMapper;
import com.stockresearch.copilot.mapper.DocumentMapper;
import com.stockresearch.copilot.mapper.DocumentSourceMapper;
import com.stockresearch.copilot.service.DocumentIngestService;
import com.stockresearch.copilot.service.DocumentService;
import com.stockresearch.copilot.service.FileStorageService;
import com.stockresearch.copilot.service.support.DocumentConverters;
import com.stockresearch.copilot.vo.DocumentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

	private final CompanyMapper companyMapper;
	private final DocumentMapper documentMapper;
	private final DocumentSourceMapper documentSourceMapper;
	private final FileStorageService fileStorageService;
	private final DocumentIngestService documentIngestService;
	private final AppProperties appProperties;

	@Override
	@Transactional
	public DocumentVO upload(Long companyId, String docType, String title, LocalDate publishDate, MultipartFile file) {
		if (companyId == null) {
			throw new BizException(ErrorCode.VALIDATION_FAILED, "companyId is required");
		}
		Company company = companyMapper.selectById(companyId);
		if (company == null) {
			throw new BizException(ErrorCode.NOT_FOUND, "company not found: " + companyId);
		}
		DocType type = DocType.from(docType);
		FileStorageService.StoredFile stored = fileStorageService.store(companyId, file);

		Document document = new Document();
		document.setCompanyId(companyId);
		document.setDocType(type.name());
		document.setTitle(resolveTitle(title, stored.originalFileName()));
		document.setFileName(stored.originalFileName());
		document.setFileExt(stored.fileExt());
		document.setFileSize(stored.fileSize());
		document.setStoragePath(stored.storagePath());
		document.setPublishDate(publishDate);
		document.setProcessStatus(ProcessStatus.UPLOADED.name());
		documentMapper.insert(document);

		DocumentSource source = new DocumentSource();
		source.setDocumentId(document.getId());
		source.setSourceType(SourceType.UPLOAD.name());
		source.setSourceName(stored.originalFileName());
		documentSourceMapper.insert(source);

		triggerIngest(document.getId());
		return DocumentConverters.toDocumentVO(documentMapper.selectById(document.getId()));
	}

	@Override
	public DocumentVO getById(Long id) {
		Document document = requireDocument(id);
		return DocumentConverters.toDocumentVO(document);
	}

	@Override
	public PageResult<DocumentVO> page(DocumentQueryRequest request) {
		Page<Document> page = new Page<>(request.getPageNum(), request.getPageSize());
		LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(request.getCompanyId() != null, Document::getCompanyId, request.getCompanyId())
				.eq(StringUtils.hasText(request.getDocType()), Document::getDocType, request.getDocType())
				.eq(StringUtils.hasText(request.getProcessStatus()), Document::getProcessStatus,
						request.getProcessStatus())
				.ge(request.getStartDate() != null, Document::getPublishDate, request.getStartDate())
				.le(request.getEndDate() != null, Document::getPublishDate, request.getEndDate())
				.like(StringUtils.hasText(request.getKeyword()), Document::getTitle, request.getKeyword())
				.orderByDesc(Document::getId);
			Page<Document> result = documentMapper.selectPage(page, wrapper);
		List<DocumentVO> records = result.getRecords().stream().map(DocumentConverters::toDocumentVO).toList();
		return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
	}

	@Override
	@Transactional
	public DocumentVO reprocess(Long id) {
		Document document = requireDocument(id);
		document.setProcessStatus(ProcessStatus.UPLOADED.name());
		document.setErrorMessage(null);
		documentMapper.updateById(document);
		triggerIngest(id);
		return DocumentConverters.toDocumentVO(documentMapper.selectById(id));
	}

	@Override
	public Resource openFile(Long id) {
		Document document = requireDocument(id);
		if (!StringUtils.hasText(document.getStoragePath())) {
			throw new BizException(ErrorCode.NOT_FOUND, "document file not found: " + id);
		}
		Path path = fileStorageService.resolve(document.getStoragePath());
		try {
			return new UrlResource(path.toUri());
		}
		catch (Exception ex) {
			throw new BizException(ErrorCode.INTERNAL_ERROR, "failed to open document file: " + ex.getMessage());
		}
	}

	private void triggerIngest(Long documentId) {
		Runnable task = () -> {
			if (appProperties.getIngest().isAsyncEnabled()) {
				documentIngestService.ingestAsync(documentId);
			}
			else {
				documentIngestService.ingest(documentId);
			}
		};
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					task.run();
				}
			});
		}
		else {
			task.run();
		}
	}

	private Document requireDocument(Long id) {
		Document document = documentMapper.selectById(id);
		if (document == null) {
			throw new BizException(ErrorCode.NOT_FOUND, "document not found: " + id);
		}
		return document;
	}

	private String resolveTitle(String title, String fileName) {
		if (StringUtils.hasText(title)) {
			return title.trim();
		}
		int idx = fileName.lastIndexOf('.');
		return idx > 0 ? fileName.substring(0, idx) : fileName;
	}
}
