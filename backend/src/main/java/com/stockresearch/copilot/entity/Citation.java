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
@TableName("citation")
public class Citation {

	@TableId(type = IdType.AUTO)
	private Long id;

	private String refType;

	private Long refId;

	private Long chunkId;

	private String quoteText;

	private Integer rankNo;

	private Double score;

	@TableField(fill = FieldFill.INSERT)
	private LocalDateTime createdAt;

	@TableLogic
	private Integer deleted;
}
