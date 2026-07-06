# spring-ai-mcp-server

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/devalgas-k/demo-devlogs-modules-21/tree/master/mcp-spring-boot-spring-ai-demo)
[![Java Version](https://img.shields.io/badge/Java-17%2B-blue)](https://adoptium.net/)
[![Spring Boot Version](https://img.shields.io/badge/Spring%20Boot-3.3.0-green)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

MCP (Model Context Protocol) Server based on Spring Boot and Spring AI. This server exposes tools, resources, and prompts to LLM clients via the standard MCP protocol.

---

## 1. Project Overview

### 1.1 Mission

This project implements an MCP server conforming to the official Model Context Protocol specification. It serves as a gateway between language models (LLMs) and various AI providers (OpenAI, Anthropic, Ollama), offering a standardized way to expose functionality to AI agents.

### 1.2 Key Features

- **Standard MCP Protocol**: Complete implementation of Model Context Protocol for LLM-server communication
- **Spring AI Integration**: Native support for multiple AI providers with automatic fallback
- **Multi-Provider Support**: OpenAI (GPT-4), Anthropic (Claude), Ollama (local, free)
- **Resilient Architecture**: Circuit breakers, retries, rate limiting for robust production
- **SSE Streaming**: Real-time responses via Server-Sent Events
- **Advanced Security**: Injection protection, rate limiting, PII-safe logging

### 1.3 Why Use This Project?

| Benefit | Description |
|---------|-------------|
| **Standardization** | Universal MCP interface for all your tools |
| **Flexibility** | Switch AI provider without modifying client code |
| **Resilience** | Survives provider outages with circuit breakers |
| **Performance** | Rate limiting and async tool execution |
| **Observability** | Prometheus metrics, distributed tracing, health checks |

---

## 2. Architecture

### 2.1 Architecture Diagram

```
+-----------------------------------------------------------------------------+
|                              CLIENT (LLM)                                    |
|                    (GPT-4, Claude, Llama, etc.)                             |
+-----------------------------------------------------------------------------+
                                     |
                                     | HTTP/SSE
                                     v
+-----------------------------------------------------------------------------+
|                         MCP SERVER ENDPOINT                                  |
|  +-------------+  +-------------+  +-------------+  +-----------------+   |
|  | POST /rpc   |  | GET /stream |  |POST /rpc/async|  | GET /health    |   |
|  | (sync)      |  | (SSE)       |  | (async)       |  | (health)        |   |
|  +-------------+  +-------------+  +-------------+  +-----------------+   |
+-----------------------------------------------------------------------------+
                                     |
                    +----------------+----------------+
                    v                v                v
+-------------------------+ +-----------------+ +---------------------+
|   TOOL REGISTRY         | |  RESOURCE       | |   PROMPT            |
|   +-------------------+ | |  REGISTRY       | |   REGISTRY          |
|   | - WeatherTool     | | |  +-----------+  | |   +---------------+  |
|   | - DatabaseTool    | | |  | /data/*   |  | |   | - assistant   |  |
|   | - SchedulerTool   | | |  | /config/* |  | |   | - developer   |  |
|   | - WebSearchTool   | | |  +-----------+  | |   | - analyst      |  |
|   +-------------------+ | +-----------------+ |   +---------------+  |
|         |               |         |           |           |          |
|         v               |         v           |           v          |
|  +-----------------+   |  +-------------+   |   +---------------+  |
|  | Tool Executor   |   |  | Resource    |   |   | Prompt        |  |
|  | (Validation,    |   |  | Loader      |   |   | Template      |  |
|  |  Sanitization)  |   |  +-------------+   |   +---------------+  |
|  +-----------------+   |                      |                       |
+-------------------------+----------------------+-----------------------+
                                     |
                    +----------------+----------------+
                    v                v                v
+-------------------------+ +-----------------+ +---------------------+
|       OPENAI            | |   ANTHROPIC     | |      OLLAMA         |
|   (GPT-4, GPT-4o)       | | (Claude 3.5)     | |   (Local, Llama3)   |
|   Base URL: api.openai  | | Base URL: api.   | | Base URL: localhost |
|                         | | anthropic.com    | | Port: 11434         |
+-------------------------+ +-----------------+ +---------------------+
                                     |
                         +-----------+-----------+
                         v                       v
                  +-------------+         +-------------+
                  | Resilience4j |         |  Bucket4j    |
                  | Circuit      |         |  Rate        |
                  | Breaker      |         |  Limiting    |
                  +-------------+         +-------------+
```

### 2.2 JSON-RPC Request Flow

```
Client                  MCP Server              Tool Registry           Provider AI
  |                         |                         |                      |
  |-- POST /mcp/rpc ------>|                         |                      |
  |  {                     |                         |                      |
  |    "jsonrpc": "2.0",  |                         |                      |
  |    "method": "tools/  |                         |                      |
  |            call",      |                         |                      |
  |    "params": {...}    |                         |                      |
  |  }                     |                         |                      |
  |                         |-- Validate & Route ---->|                      |
  |                         |                         |                      |
  |                         |<-- Tool Definition -----|                      |
  |                         |                         |                      |
  |                         |-- Execute Tool -------->|                      |
  |                         |                         |                      |
  |                         |<-- Tool Result ---------|                      |
  |                         |                         |                      |
  |<-- JSON-RPC Response ---|                         |                      |
  |  {                     |                         |                      |
  |    "jsonrpc": "2.0",   |                         |                      |
  |    "result": {...}     |                         |                      |
  |  }                     |                         |                      |
```

---

## 3. Prerequisites

### 3.1 Minimum Requirements

| Software | Minimum Version | Description |
|----------|-----------------|-------------|
| **Java (JDK)** | 17+ | Runtime environment |
| **Maven** | 3.8+ | Build tool |
| **Git** | 2.0+ | Version control |

### 3.2 API Keys and Access

#### Option A: OpenAI (Recommended for Production)

Get your API key at: https://platform.openai.com/api-keys

```bash
export OPENAI_API_KEY=sk-...
```

#### Option B: Anthropic (Fallback)

Get your API key at: https://console.anthropic.com/

```bash
export ANTHROPIC_API_KEY=sk-ant-...
```

#### Option C: Ollama (Local Development, Free)

See section 3.3 below for installation.

### 3.3 Ollama Installation

#### Docker (Recommended)

```bash
# Pull and run Ollama container
docker run -d \
  --name ollama \
  -p 11434:11434 \
  ollama/ollama:latest

# Pull a model (e.g., llama3)
docker exec ollama ollama pull llama3

# Verify
docker exec ollama ollama list
```

#### Mac/Linux (Native)

```bash
# Installation via curl
curl -fsSL https://ollama.ai/install.sh | sh

# Download a model
ollama pull llama3

# Verify
ollama list
```

The server will be accessible at `http://localhost:11434`.

---

## 4. Installation and Execution

### 4.1 Clone the Project

```bash
git clone https://github.com/devalgas-k/demo-devlogs-modules-21/tree/master/mcp-spring-boot-spring-ai-demo.git
cd spring-ai-mcp-server
```

### 4.2 Build with Maven

```bash
# Compile and test
./mvnw clean package

# Build without tests (fast dev)
./mvnw clean package -DskipTests

# Build with production profile
./mvnw clean package -Pprod
```

### 4.3 Execution

#### Development Mode (with API keys)

```bash
export OPENAI_API_KEY=sk-your-key-here
./mvnw spring-boot:run
```

#### Production Mode

```bash
java -jar target/spring-ai-mcp-server-1.0.0.jar \
  --spring.profiles.active=prod \
  --OPENAI_API_KEY=sk-your-key-here
```

#### With Docker

```bash
# Build the image
docker build -t spring-ai-mcp-server .

# Run
docker run -p 8080:8080 \
  -e OPENAI_API_KEY=sk-your-key-here \
  spring-ai-mcp-server
```

### 4.4 Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `OPENAI_API_KEY` | OpenAI API Key | - |
| `ANTHROPIC_API_KEY` | Anthropic API Key | - |
| `OLLAMA_BASE_URL` | Ollama server URL | `http://localhost:11434` |
| `SERVER_PORT` | Server port | `8080` |
| `LOG_FILE_PATH` | Log file path | `/var/log/...` |

---

## 5. Configuration

### 5.1 application.yml Structure

```yaml
spring:
  application:
    name: spring-ai-mcp-server

  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: ${OPENAI_BASE_URL:https://api.openai.com/v1}
      chat-model: ${OPENAI_CHAT_MODEL:gpt-4o}
      timeout: ${OPENAI_TIMEOUT:60s}

    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
      base-url: ${ANTHROPIC_BASE_URL:https://api.anthropic.com}
      chat-model: ${ANTHROPIC_CHAT_MODEL:claude-3-5-sonnet-20240620}

    ollama:
      base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
      chat-model: ${OLLAMA_CHAT_MODEL:llama3}

mcp:
  server:
    name: spring-ai-mcp-server
    version: 1.0.0
  transport: http-sse
  tool:
    default-timeout: 30s
    max-timeout: 120s

resilience4j:
  circuitbreaker:
    configs:
      default:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
```

### 5.2 Spring Profiles

#### Development Profile

```yaml
spring:
  config:
    activate:
      on-profile: development

logging:
  level:
    com.example.mcp: DEBUG
    org.springframework.ai: DEBUG
```

#### Production Profile

```yaml
spring:
  config:
    activate:
      on-profile: production

server:
  ssl:
    enabled: true

logging:
  level:
    com.example.mcp: INFO
    root: WARN

management:
  tracing:
    enabled: true
```

### 5.3 Profile Activation

```bash
# Development
./mvnw spring-boot:run -Dspring-boot.run.profiles=development

# Production
java -jar target/spring-ai-mcp-server-1.0.0.jar --spring.profiles.active=production
```

---

## 6. Endpoints

### 6.1 MCP Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/mcp/rpc` | Synchronous JSON-RPC requests |
| `GET` | `/mcp/stream` | SSE streaming for server notifications |
| `POST` | `/mcp/rpc/async` | Async operations with session |
| `GET` | `/mcp/health` | Server health check |

### 6.2 Actuator Endpoints (Monitoring)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/actuator/health` | Full health check |
| `GET` | `/actuator/info` | Application information |
| `GET` | `/actuator/prometheus` | Prometheus metrics |
| `GET` | `/actuator/metrics` | Detailed metrics |
| `GET` | `/actuator/circuitbreakers` | Circuit breaker status |
| `GET` | `/actuator/retries` | Retry status |

### 6.3 Usage Examples

#### Synchronous JSON-RPC Request

```bash
curl -X POST http://localhost:8080/mcp/rpc \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
      "name": "weather",
      "arguments": {
        "location": "Paris",
        "unit": "celsius"
      }
    },
    "id": 1
  }'
```

#### SSE Streaming

```bash
curl -N http://localhost:8080/mcp/stream?sessionId=test-session
```

#### Health Check

```bash
curl http://localhost:8080/mcp/health
# Response: {"status":"UP","activeSessions":5,"sseConnections":3}
```

---

## 7. Available Tools

### 7.1 WeatherTool

**Name**: `weather`

**Description**: Retrieves current weather information for a specified location.

**Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `location` | string | Yes | City name (max 200 chars) |
| `unit` | string | No | `celsius` or `fahrenheit` (default: celsius) |

**Example**:
```json
{
  "name": "weather",
  "arguments": {
    "location": "Paris",
    "unit": "celsius"
  }
}
```

**Response**:
```json
{
  "location": "Paris",
  "temperature": 22,
  "unit": "celsius",
  "condition": "partly_cloudy",
  "humidity": 65,
  "windSpeed": 12
}
```

### 7.2 DatabaseTool

**Name**: `database`

**Description**: Execute read-only SQL queries on the configured database.

**Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `query` | string | Yes | SELECT queries only |
| `maxRows` | integer | No | Max rows (default: 100, max: 1000) |

**Security**:
- Blocked: `DROP`, `DELETE`, `INSERT`, `UPDATE`, `TRUNCATE`, `ALTER`, `CREATE`
- Blocked: SQL comments (`--`, `/*`, `*/`)
- Blocked: UNION-based injection patterns
- Allowed: SELECT queries only

**Example**:
```json
{
  "name": "database",
  "arguments": {
    "query": "SELECT * FROM users WHERE active = true",
    "maxRows": 50
  }
}
```

### 7.3 SchedulerTool

**Name**: `scheduler`

**Description**: Schedule future task execution using cron expressions.

**Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `schedule` | string | Yes | Cron expression (5-6 fields) |
| `action` | string | Yes | Action to execute |
| `payload` | string | No | Additional data |

**Supported Actions**:
- `NOTIFY` - Send notification
- `SYNC` - Synchronization operation
- `CLEANUP` - Cleanup task

**Example**:
```json
{
  "name": "scheduler",
  "arguments": {
    "schedule": "0 0 9 * * MON-FRI",
    "action": "NOTIFY",
    "payload": "Daily report generation"
  }
}
```

### 7.4 WebSearchTool

**Name**: `websearch`

**Description**: Search the web for information.

**Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `query` | string | Yes | Search term (2-500 chars) |
| `limit` | integer | No | Number of results (default: 10, max: 50) |

**Example**:
```json
{
  "name": "websearch",
  "arguments": {
    "query": "Spring Boot best practices 2024",
    "limit": 10
  }
}
```

---

## 8. Technical Choices

### 8.1 Technology Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| **Spring Boot** | 3.3.0 | Application framework |
| **Spring AI** | 1.0.0-M4 | Multi-vendor AI integration |
| **Java** | 17+ | Programming language |
| **Resilience4j** | 2.2.0 | Circuit breakers, retries, bulkhead |
| **Bucket4j** | 8.7.0 | Rate limiting |
| **Micrometer** | - | Metrics (Prometheus) |
| **OpenTelemetry** | - | Distributed tracing |
| **WebFlux** | - | Reactive SSE support |

### 8.2 Resilience Patterns

#### Circuit Breaker (Resilience4j)

```yaml
circuitbreaker:
  instances:
    openai:
      sliding-window-size: 10
      failure-rate-threshold: 50
      wait-duration-in-open-state: 30s
```

#### Retry with Exponential Backoff

```yaml
retry:
  max-attempts: 3
  wait-duration: 1s
  enable-exponential-backoff: true
  exponential-backoff-multiplier: 2.0
```

#### Bulkhead (Parallel Limitation)

```yaml
bulkhead:
  llmCalls:
    max-concurrent-calls: 10
```

### 8.3 SSE Transport

The server uses Server-Sent Events (SSE) for bidirectional streaming:

- **Persistent connections**: Long-polling friendly
- **Heartbeat**: 30s to detect orphaned connections
- **Max connections**: 1000 simultaneous connections
- **Session management**: Configurable timeout (10min default)

---

## 9. Security

### 9.1 Injection Protection

#### Prompt Injection Prevention

```java
// InputSanitizer.java - Input validation and sanitization
public String sanitize(String input) {
    // Remove suspicious patterns
    // Escape special characters
    // Validate maximum length
}
```

#### SQL Injection Prevention (DatabaseTool)

```java
// Blocked patterns
DROP|DELETE|INSERT|UPDATE|TRUNCATE|ALTER|CREATE|EXEC|GRANT

// UNION injection detection
UNION\s+(ALL\s+)?SELECT

// Comment injection
(/\*|\*/|--|;)
```

### 9.2 Rate Limiting (Bucket4j)

```yaml
mcp:
  security:
    rate-limit:
      requests-per-minute: 100
      burst-capacity: 20
```

### 9.3 PII-Safe Logging

```java
// Mask sensitive data
private String maskLocation(String location) {
    if (location.length() <= 4) return "***";
    return location.substring(0, 2) + "***" + location.substring(location.length() - 2);
}

// Structured JSON logging (no PII in clear)
log.info("[{}] Weather request for location: {}", 
    correlationId, maskLocation(location));
```

### 9.4 HTTPS Enforcement

```yaml
server:
  ssl:
    enabled: true
    key-store: ${SSL_KEYSTORE_PATH}
    key-store-type: PKCS12
```

---

## 10. Monitoring

### 10.1 Actuator Endpoints

```bash
# Full health check
curl http://localhost:8080/actuator/health

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# Circuit breakers
curl http://localhost:8080/actuator/circuitbreakers

# Retries
curl http://localhost:8080/actuator/retries
```

### 10.2 Custom Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `mcp.tools.calls` | Counter | Number of tool calls |
| `mcp.tools.duration` | Timer | Tool execution time |
| `mcp.sessions.active` | Gauge | Active sessions |
| `mcp.connections.sse` | Gauge | Active SSE connections |
| `resilience4j.circuitbreaker` | Various | Circuit breaker states |

### 10.3 Health Checks

```bash
# Liveness probe (Kubernetes)
curl http://localhost:8080/actuator/health/liveness

# Readiness probe (Kubernetes)
curl http://localhost:8080/actuator/health/readiness
```

### 10.4 Grafana/Prometheus Integration

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'spring-ai-mcp-server'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
```

---

## 11. Contribution

### 11.1 Environment Setup

```bash
# Fork the repository
git clone https://github.com/your-fork/spring-ai-mcp-server.git
cd spring-ai-mcp-server

# Add upstream remote
git remote add upstream https://github.com/devalgas-k/demo-devlogs-modules-21/tree/master/mcp-spring-boot-spring-ai-demo.git

# Create feature branch
git checkout -b feature/new-feature
```

### 11.2 Code Standards

- **Style**: Follow Spring Boot conventions
- **Tests**: Minimum 80% coverage for tools
- **Commits**: Conventional Commits (`feat:`, `fix:`, `docs:`)
- **Pull Requests**: Detailed description, link to issue

### 11.3 Commit Structure

```
feat: add new tool
fix: fix error handling
docs: update documentation
refactor: improve session management
test: add integration tests
chore: update dependencies
```

### 11.4 Tests

```bash
# Run all tests
./mvnw test

# Tests with coverage
./mvnw test jacoco:report

# Integration tests (requires providers)
./mvnw verify -Pintegration-tests
```

### 11.5 PR Checklist

- [ ] Unit tests added/updated
- [ ] API documentation updated
- [ ] Environment variables documented
- [ ] No compilation errors
- [ ] SonarQube (if configured) passes

---

## 12. License

This project is distributed under the MIT license. See [LICENSE](LICENSE) for details.

```
MIT License

Copyright (c) 2024 spring-ai-mcp-server contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## Useful Links

- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [MCP Specification](https://modelcontextprotocol.io/)
- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/actuator-api/html/)
