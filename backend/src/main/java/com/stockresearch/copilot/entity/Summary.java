package com.stockresearch.copilot.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("summary")
public class Summary {

	@TableId(type = IdType.AUTO)
	private Long id;

	private Long companyId;

	private String stockCode;

	private String mode;

	private String title;

	private String overview;

	private String sectionsJson;

	private String docTypesJson;

	private LocalDate startDate;

	private LocalDate endDate;

	private Long latencyMs;

	private String status;

	@TableField(fill = FieldFill.INSERT)
	private LocalDateTime createdAt;

	@TableLogic
	private Integer deleted;
}
