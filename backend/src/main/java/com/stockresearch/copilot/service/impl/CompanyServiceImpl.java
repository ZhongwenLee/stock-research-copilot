package com.stockresearch.copilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stockresearch.copilot.common.exception.BizException;
import com.stockresearch.copilot.common.exception.ErrorCode;
import com.stockresearch.copilot.config.CacheConfig;
import com.stockresearch.copilot.dto.CompanyCreateRequest;
import com.stockresearch.copilot.entity.Company;
import com.stockresearch.copilot.mapper.CompanyMapper;
import com.stockresearch.copilot.service.CompanyService;
import com.stockresearch.copilot.vo.CompanyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

	private final CompanyMapper companyMapper;

	@Override
	@Transactional
	@CacheEvict(cacheNames = CacheConfig.COMPANIES, allEntries = true)
	public CompanyVO create(CompanyCreateRequest request) {
		Long exists = companyMapper.selectCount(new LambdaQueryWrapper<Company>()
				.eq(Company::getStockCode, request.getStockCode().trim()));
		if (exists != null && exists > 0) {
			throw new BizException(ErrorCode.BAD_REQUEST, "stockCode already exists: " + request.getStockCode());
		}
		Company company = new Company();
		company.setStockCode(request.getStockCode().trim());
		company.setName(request.getName().trim());
		company.setExchange(request.getExchange());
		company.setIndustry(request.getIndustry());
		company.setStatus("ACTIVE");
		companyMapper.insert(company);
		return toVO(company);
	}

	@Override
	@Cacheable(cacheNames = CacheConfig.COMPANIES, key = "'id:' + #id")
	public CompanyVO getById(Long id) {
		Company company = companyMapper.selectById(id);
		if (company == null) {
			throw new BizException(ErrorCode.NOT_FOUND, "company not found: " + id);
		}
		return toVO(company);
	}

	@Override
	@Cacheable(cacheNames = CacheConfig.COMPANIES, key = "'all'")
	public List<CompanyVO> listAll() {
		return companyMapper.selectList(new LambdaQueryWrapper<Company>().orderByAsc(Company::getId))
				.stream()
				.map(this::toVO)
				.toList();
	}

	private CompanyVO toVO(Company company) {
		return CompanyVO.builder()
				.id(company.getId())
				.stockCode(company.getStockCode())
				.name(company.getName())
				.exchange(company.getExchange())
				.industry(company.getIndustry())
				.status(company.getStatus())
				.build();
	}
}
