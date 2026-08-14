package com.stockresearch.copilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stockresearch.copilot.entity.Citation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CitationMapper extends BaseMapper<Citation> {
}
