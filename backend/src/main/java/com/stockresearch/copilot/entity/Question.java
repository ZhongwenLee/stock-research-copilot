package com.stockresearch.copilot.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("question")
public class Question {

	@TableId(type = IdType.AUTO)
	private Long id;

	private Long companyId;

	private String questionText;

	private String answerText;

	private String intentType;

	private Long latencyMs;

	@TableField(fill = FieldFill.INSERT)
	private LocalDateTime createdAt;

	@TableLogic
	private Integer deleted;
}
