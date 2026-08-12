package com.stockresearch.copilot.dto;

import com.stockresearch.copilot.common.result.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentQueryRequest extends PageQuery {

	private Long companyId;

	private String docType;

	private String processStatus;
}
