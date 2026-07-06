package com.example.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Main entry point for the MCP Spring Boot server.
 * 
 * This application provides a Model Context Protocol (MCP) server
 * that integrates with Spring AI to expose tools, resources, and prompts
 * to LLM clients.
 */
@SpringBootApplication
@EnableConfigurationProperties
public class McpServerApplication {

    private static final Logger log = LoggerFactory.getLogger(McpServerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
        log.info("MCP Server started successfully");
    }
}
