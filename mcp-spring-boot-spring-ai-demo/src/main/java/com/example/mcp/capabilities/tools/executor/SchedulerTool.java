package com.example.mcp.capabilities.tools.executor;

import com.example.mcp.capabilities.tools.annotation.McpTool;
import com.example.mcp.capabilities.tools.response.ToolResponse;
import com.example.mcp.capabilities.tools.response.ToolResponse.ErrorCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Task scheduler tool for scheduling future tool execution.
 * Demonstrates server-initiated requests (SSE/push notifications).
 */
@Component
public class SchedulerTool implements ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(SchedulerTool.class);
    
    private final TaskScheduler taskScheduler;
    private final ScheduledExecutorService executorService;
    private final ConcurrentHashMap<String, ScheduledTaskInfo> scheduledTasks;
    private final AtomicLong taskIdGenerator;
    
    public SchedulerTool(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
        this.executorService = Executors.newScheduledThreadPool(4);
        this.scheduledTasks = new ConcurrentHashMap<>();
        this.taskIdGenerator = new AtomicLong(System.currentTimeMillis());
    }
    
    @McpTool(name = "scheduler", description = "Schedule future task execution with cron expressions")
    public ToolResponse execute(
            @NotBlank(message = "Schedule (cron expression) is required") 
            @Pattern(regexp = "^[\\w\\-,\\s\\*\\/]+$", 
                     message = "Invalid cron expression format") String schedule,
            @NotBlank(message = "Action is required") String action,
            String payload) {
        
        String correlationId = generateCorrelationId();
        String taskId = generateTaskId();
        
        log.info("[{}] Scheduler request received - taskId: {}, action: {}", 
                correlationId, taskId, action);
        
        try {
            validateCronExpression(schedule);
            validateAction(action);
            
            CronTrigger cronTrigger = new CronTrigger(schedule);
            Instant nextExecution = cronTrigger.nextExecution(
                    java.util.Calendar.getInstance().toInstant().atZone(
                            java.util.Calendar.getInstance().getTimeZone().toZoneId()));
            
            // Create scheduled task
            ScheduledFuture<?> scheduledFuture = taskScheduler.schedule(
                    () -> executeScheduledAction(action, payload, correlationId),
                    cronTrigger);
            
            ScheduledTaskInfo taskInfo = new ScheduledTaskInfo(
                    taskId, action, schedule, nextExecution, 
                    payload, scheduledFuture);
            scheduledTasks.put(taskId, taskInfo);
            
            log.info("[{}] Task scheduled successfully - taskId: {}, nextExecution: {}", 
                    correlationId, taskId, nextExecution);
            
            return ToolResponse.success(Map.of(
                "taskId", taskId,
                "status", "SCHEDULED",
                "nextExecution", nextExecution.toString(),
                "cronExpression", schedule,
                "action", action
            ));
            
        } catch (IllegalArgumentException e) {
            log.warn("[{}] Invalid cron expression: {}", correlationId, e.getMessage());
            return ToolResponse.error(ErrorCode.VALIDATION_ERROR, 
                    "Invalid cron expression: " + e.getMessage());
        } catch (RejectedExecutionException e) {
            log.error("[{}] Task scheduling rejected: {}", correlationId, e.getMessage());
            return ToolResponse.error(ErrorCode.SERVICE_UNAVAILABLE, 
                    "Scheduler service unavailable");
        } catch (Exception e) {
            log.error("[{}] Unexpected error during task scheduling", correlationId, e);
            return ToolResponse.error(ErrorCode.INTERNAL_ERROR, 
                    "An unexpected error occurred");
        }
    }
    
    /**
     * Schedule a one-time delayed task.
     */
    public CompletableFuture<ToolResponse> scheduleDelayedTask(
            String action, String payload, long delayMs) {
        
        String taskId = generateTaskId();
        String correlationId = generateCorrelationId();
        
        log.info("[{}] Delayed task requested - taskId: {}, delay: {}ms", 
                correlationId, taskId, delayMs);
        
        CompletableFuture<ToolResponse> future = new CompletableFuture<>();
        
        ScheduledFuture<?> scheduledFuture = executorService.schedule(
                () -> {
                    try {
                        ToolResponse response = executeScheduledAction(
                                action, payload, correlationId);
                        future.complete(response);
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                },
                delayMs,
                TimeUnit.MILLISECONDS);
        
        ScheduledTaskInfo taskInfo = new ScheduledTaskInfo(
                taskId, action, null, 
                Instant.now().plusMillis(delayMs), payload, scheduledFuture);
        scheduledTasks.put(taskId, taskInfo);
        
        return CompletableFuture.completedFuture(
                ToolResponse.success(Map.of(
                        "taskId", taskId,
                        "status", "SCHEDULED",
                        "scheduledTime", Instant.now().plusMillis(delayMs).toString(),
                        "delayMs", delayMs
                )));
    }
    
    /**
     * Cancel a scheduled task.
     */
    public ToolResponse cancelTask(String taskId) {
        String correlationId = generateCorrelationId();
        
        log.info("[{}] Cancel task request - taskId: {}", correlationId, taskId);
        
        ScheduledTaskInfo taskInfo = scheduledTasks.get(taskId);
        if (taskInfo == null) {
            return ToolResponse.error(ErrorCode.NOT_FOUND, 
                    "Task not found: " + taskId);
        }
        
        boolean cancelled = taskInfo.future().cancel(false);
        
        if (cancelled) {
            scheduledTasks.remove(taskId);
            log.info("[{}] Task cancelled successfully: {}", correlationId, taskId);
            return ToolResponse.success(Map.of(
                    "taskId", taskId,
                    "status", "CANCELLED"
            ));
        } else {
            log.warn("[{}] Failed to cancel task (may be running): {}", 
                    correlationId, taskId);
            return ToolResponse.error(ErrorCode.OPERATION_FAILED, 
                    "Task could not be cancelled");
        }
    }
    
    /**
     * Get status of all scheduled tasks.
     */
    public Map<String, Object> getScheduledTasksStatus() {
        Map<String, Object> status = new java.util.HashMap<>();
        status.put("totalTasks", scheduledTasks.size());
        status.put("tasks", scheduledTasks.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().toMap()
                )));
        return status;
    }
    
    private ToolResponse executeScheduledAction(
            String action, String payload, String parentCorrelationId) {
        
        String taskCorrelationId = parentCorrelationId + "-exec";
        log.info("[{}] Executing scheduled action: {}", taskCorrelationId, action);
        
        try {
            // Validate action
            validateAction(action);
            
            // Process action based on type
            Object result = processAction(action, payload, taskCorrelationId);
            
            log.info("[{}] Scheduled action completed successfully", 
                    taskCorrelationId);
            
            return ToolResponse.success(Map.of(
                    "action", action,
                    "result", result != null ? result : "completed",
                    "executedAt", ZonedDateTime.now().toString()
            ));
            
        } catch (Exception e) {
            log.error("[{}] Scheduled action failed: {}", 
                    taskCorrelationId, e.getMessage());
            return ToolResponse.error(ErrorCode.EXECUTION_FAILED, 
                    "Action execution failed: " + e.getMessage());
        }
    }
    
    private Object processAction(String action, String payload, String correlationId) {
        // Action processor - extend based on supported actions
        return switch (action.toUpperCase()) {
            case "NOTIFY" -> processNotification(payload, correlationId);
            case "SYNC" -> processSync(payload, correlationId);
            case "CLEANUP" -> processCleanup(payload, correlationId);
            default -> processGenericAction(action, payload, correlationId);
        };
    }
    
    private Object processNotification(String payload, String correlationId) {
        // Placeholder for notification processing
        log.debug("[{}] Processing notification with payload size: {} bytes", 
                correlationId, 
                payload != null ? payload.length() : 0);
        return Map.of("notified", true);
    }
    
    private Object processSync(String payload, String correlationId) {
        log.debug("[{}] Processing sync operation", correlationId);
        return Map.of("synced", true);
    }
    
    private Object processCleanup(String payload, String correlationId) {
        log.debug("[{}] Processing cleanup operation", correlationId);
        return Map.of("cleaned", true);
    }
    
    private Object processGenericAction(
            String action, String payload, String correlationId) {
        log.debug("[{}] Processing generic action: {}", correlationId, action);
        return Map.of("action", action, "processed", true);
    }
    
    private void validateCronExpression(String cron) {
        if (cron == null || cron.trim().isEmpty()) {
            throw new IllegalArgumentException("Cron expression cannot be empty");
        }
        
        // Basic validation - Spring's CronTrigger will do full validation
        String[] parts = cron.trim().split("\\s+");
        if (parts.length < 5) {
            throw new IllegalArgumentException(
                    "Cron expression must have at least 5 fields");
        }
        if (parts.length > 6) {
            throw new IllegalArgumentException(
                    "Cron expression cannot have more than 6 fields");
        }
    }
    
    private void validateAction(String action) {
        if (action == null || action.trim().isEmpty()) {
            throw new IllegalArgumentException("Action cannot be empty");
        }
        if (action.length() > 100) {
            throw new IllegalArgumentException("Action name too long");
        }
    }
    
    private String generateCorrelationId() {
        return "sch-" + System.currentTimeMillis() + "-" + 
               Integer.toHexString((int)(Math.random() * 0xFFFF));
    }
    
    private String generateTaskId() {
        return "task-" + taskIdGenerator.incrementAndGet();
    }
    
    // Inner class for scheduled task info
    private record ScheduledTaskInfo(
            String taskId,
            String action,
            String cronExpression,
            Instant nextExecution,
            String payload,
            ScheduledFuture<?> future
    ) {
        Map<String, Object> toMap() {
            return Map.of(
                    "taskId", taskId,
                    "action", action,
                    "cronExpression", cronExpression != null ? 
                            cronExpression : "ONE_TIME",
                    "nextExecution", nextExecution.toString(),
                    "isDone", future.isDone(),
                    "isCancelled", future.isCancelled()
            );
        }
    }
}
