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
@TableName("document")
public class Document {

	@TableId(type = IdType.AUTO)
	private Long id;

	private Long companyId;

	private String title;

	private String docType;

	private String fileName;

	private String fileExt;

	private Long fileSize;

	private String storagePath;

	private LocalDate publishDate;

	private String processStatus;

	private String errorMessage;

	@TableField(fill = FieldFill.INSERT)
	private LocalDateTime createdAt;

	@TableField(fill = FieldFill.INSERT_UPDATE)
	private LocalDateTime updatedAt;

	@TableLogic
	private Integer deleted;
}
