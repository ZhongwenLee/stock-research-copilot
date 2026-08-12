package com.stockresearch.copilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stockresearch.copilot.entity.Company;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CompanyMapper extends BaseMapper<Company> {
}
