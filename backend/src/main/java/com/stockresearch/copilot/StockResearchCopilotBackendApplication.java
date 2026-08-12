package com.stockresearch.copilot;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {MybatisPlusAutoConfiguration.class})
public class StockResearchCopilotBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(StockResearchCopilotBackendApplication.class, args);
	}

}
