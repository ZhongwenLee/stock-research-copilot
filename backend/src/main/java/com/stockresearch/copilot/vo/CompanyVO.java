package com.stockresearch.copilot.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompanyVO {

	private Long id;
	private String stockCode;
	private String name;
	private String exchange;
	private String industry;
	private String status;
}
