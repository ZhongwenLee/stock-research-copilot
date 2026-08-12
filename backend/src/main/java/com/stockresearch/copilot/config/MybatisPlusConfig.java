package com.stockresearch.copilot.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * Manual MyBatis-Plus wiring for Spring Boot 4.x compatibility.
 * Auto-config currently hits PropertyMapper API mismatch.
 */
@Configuration
@MapperScan("com.stockresearch.copilot.mapper")
public class MybatisPlusConfig {

	@Bean
	public MybatisPlusInterceptor mybatisPlusInterceptor() {
		MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
		interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
		return interceptor;
	}

	@Bean
	public SqlSessionFactory sqlSessionFactory(DataSource dataSource,
			MybatisPlusInterceptor mybatisPlusInterceptor,
			MetaObjectHandler metaObjectHandler) throws Exception {
		MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
		factoryBean.setDataSource(dataSource);
		factoryBean.setPlugins(mybatisPlusInterceptor);

		MybatisConfiguration configuration = new MybatisConfiguration();
		configuration.setMapUnderscoreToCamelCase(true);
		configuration.setLogImpl(org.apache.ibatis.logging.slf4j.Slf4jImpl.class);
		factoryBean.setConfiguration(configuration);

		GlobalConfig globalConfig = new GlobalConfig();
		GlobalConfig.DbConfig dbConfig = new GlobalConfig.DbConfig();
		dbConfig.setLogicDeleteField("deleted");
		dbConfig.setLogicDeleteValue("1");
		dbConfig.setLogicNotDeleteValue("0");
		globalConfig.setDbConfig(dbConfig);
		globalConfig.setMetaObjectHandler(metaObjectHandler);
		factoryBean.setGlobalConfig(globalConfig);

		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		Resource[] mapperLocations = resolver.getResources("classpath*:/mapper/**/*.xml");
		if (mapperLocations.length > 0) {
			factoryBean.setMapperLocations(mapperLocations);
		}
		return factoryBean.getObject();
	}

	@Bean
	public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
		return new SqlSessionTemplate(sqlSessionFactory);
	}
}
