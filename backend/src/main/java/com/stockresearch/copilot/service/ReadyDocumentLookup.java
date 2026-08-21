package com.stockresearch.copilot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stockresearch.copilot.common.enums.DocType;
import com.stockresearch.copilot.common.enums.ProcessStatus;
import com.stockresearch.copilot.config.CacheConfig;
import com.stockresearch.copilot.entity.Document;
import com.stockresearch.copilot.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cached lookup of READY document IDs for retrieval hot path.
 */
@Service
@RequiredArgsConstructor
public class ReadyDocumentLookup {

	private final DocumentMapper documentMapper;

	@Cacheable(cacheNames = CacheConfig.READY_DOCS, key = "#companyId + ':' + (#docTypes == null ? 'ALL' : #docTypes.toString())")
	public Set<Long> findReadyDocumentIds(Long companyId, List<DocType> docTypes) {
		LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<Document>()
				.eq(Document::getProcessStatus, ProcessStatus.READY.name());
		if (companyId != null) {
			wrapper.eq(Document::getCompanyId, companyId);
		}
		if (docTypes != null && !docTypes.isEmpty()) {
			wrapper.in(Document::getDocType, docTypes.stream().map(DocType::name).toList());
		}
		return documentMapper.selectList(wrapper).stream()
				.map(Document::getId)
				.collect(Collectors.toCollection(HashSet::new));
	}

	@CacheEvict(cacheNames = CacheConfig.READY_DOCS, allEntries = true)
	public void evictAll() {
		// no-op: annotation clears cache
	}
}
