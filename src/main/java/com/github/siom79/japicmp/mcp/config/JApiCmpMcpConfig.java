package com.github.siom79.japicmp.mcp.config;

import com.github.siom79.japicmp.mcp.service.JApiCmpToolService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JApiCmpMcpConfig {

    @Bean
    ToolCallbackProvider japicmpTools(JApiCmpToolService toolService) {
        return MethodToolCallbackProvider.builder().toolObjects(toolService).build();
    }
}
