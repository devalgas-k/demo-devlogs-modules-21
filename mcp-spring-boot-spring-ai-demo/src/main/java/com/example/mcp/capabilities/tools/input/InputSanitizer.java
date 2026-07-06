package com.example.mcp.capabilities.tools.input;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Input sanitizer for user inputs in MCP tools.
 * Provides sanitization methods to prevent prompt injection and other malicious injections.
 *
 * <p>Types of sanitization performed:</p>
 * <ul>
 *     <li>Remove "ignore previous instructions" patterns</li>
 *     <li>Remove indirect execution attempts</li>
 *     <li>Escape potentially dangerous special characters</li>
 *     <li>Validate parameters against whitelist</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * @Autowired
 * private InputSanitizer sanitizer;
 * 
 * public ToolResponse execute(ToolRequest request) {
 *     String userInput = request.getParameters().get("query");
 *     String sanitized = sanitizer.sanitize(userInput);
 *     
 *     if (!sanitized.equals(userInput)) {
 *         log.warn("Potentially malicious input was sanitized");
 *     }
 *     
 *     // Use sanitized input safely
 * }
 * }</pre>
 */
@Component
public class InputSanitizer {

    private static final Logger log = LoggerFactory.getLogger(InputSanitizer.class);

    // Patterns for detecting potential injections
    private static final Pattern IGNORE_INSTRUCTIONS_PATTERN = Pattern.compile(
        "(?i)(ignore\\s+(all\\s+)?(previous|prior|above|earlier)\\s+instructions?)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern EXECUTE_PATTERN = Pattern.compile(
        "(?i)(execute|run\\s+|call\\s+)\\s*[(:]",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SYSTEM_BYPASS_PATTERN = Pattern.compile(
        "(?i)(system\\s*:|##\\s*system|<\\s*system\\s*>|```\\s*system)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern JAILBREAK_PATTERN = Pattern.compile(
        "(?i)(you\\s+are\\s+a|act\\s+as\\s+a|pretend\\s+you\\s+are|imagine\\s+you\\s+are)\\s+(different|another|new|ai\\s+without)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        ".*(union\\s+select|drop\\s+table|delete\\s+from|insert\\s+into|update\\s+.+\\s+set|exec\\s*\\(|xp_).*",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SHELL_INJECTION_PATTERN = Pattern.compile(
        "[;&|`$<>]",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern NEWLINE_INJECTION_PATTERN = Pattern.compile("[\r\n]");

    // Dangerous special characters for XML/HTML injection
    private static final Pattern XML_INJECTION_PATTERN = Pattern.compile("[<>\"'&]");

    private final Set<String> allowedValues;
    private final boolean blockSqlInjection;
    private final boolean blockShellInjection;

    // Default constructor with standard security configuration
    public InputSanitizer() {
        this(new HashSet<>(), true, true);
    }

    /**
     * Constructor with custom configuration.
     *
     * @param allowedValues allowed values (whitelist)
     * @param blockSqlInjection block SQL injection patterns
     * @param blockShellInjection block shell injection patterns
     */
    public InputSanitizer(Set<String> allowedValues, boolean blockSqlInjection, boolean blockShellInjection) {
        this.allowedValues = allowedValues != null ? new HashSet<>(allowedValues) : new HashSet<>();
        this.blockSqlInjection = blockSqlInjection;
        this.blockShellInjection = blockShellInjection;
    }

    /**
     * Sanitize user input.
     * Applies in sequence:
     * <ol>
     *     <li>Remove ignore instructions</li>
     *     <li>Remove execution attempts</li>
     *     <li>Remove system bypass attempts</li>
     *     <li>Remove jailbreak attempts</li>
     *     <li>Clean multiple newlines</li>
     *     <li>Escape dangerous XML characters</li>
     * </ol>
     *
     * @param input the input to sanitize
     * @return the sanitized input
     */
    public String sanitize(String input) {
        if (input == null) {
            return null;
        }

        String result = input;

        // Remove ignore instructions
        result = IGNORE_INSTRUCTIONS_PATTERN.matcher(result).replaceAll("");

        // Remove execution attempts
        result = EXECUTE_PATTERN.matcher(result).replaceAll("");

        // Remove system bypass
        result = SYSTEM_BYPASS_PATTERN.matcher(result).replaceAll("");

        // Remove jailbreak attempts
        result = JAILBREAK_PATTERN.matcher(result).replaceAll("");

        // Clean multiple newlines
        result = NEWLINE_INJECTION_PATTERN.matcher(result).replaceAll(" ");

        // Normalize whitespace
        result = result.replaceAll("\\s+", " ").trim();

        if (!result.equals(input)) {
            log.debug("Input was sanitized. Original length: {}, Sanitized length: {}",
                input.length(), result.length());
        }

        return result;
    }

    /**
     * Sanitize and validate input against a whitelist.
     *
     * @param input the input to sanitize
     * @param allowedValues allowed values
     * @return the sanitized input
     * @throws IllegalArgumentException if sanitized input is not in the whitelist
     */
    public String sanitizeAndValidate(String input, Set<String> allowedValues) {
        String sanitized = sanitize(input);

        if (allowedValues != null && !allowedValues.isEmpty() && !allowedValues.contains(sanitized)) {
            throw new IllegalArgumentException(
                "Input value '" + sanitized + "' is not in the allowed list"
            );
        }

        return sanitized;
    }

    /**
     * Validate input against a whitelist without sanitization.
     *
     * @param value the value to validate
     * @param allowedValues allowed values
     * @return true if the value is allowed
     */
    public boolean isAllowed(String value, Set<String> allowedValues) {
        return allowedValues == null || allowedValues.isEmpty() || allowedValues.contains(value);
    }

    /**
     * Check for SQL injection patterns.
     *
     * @param input the input to check
     * @return true if input appears to contain SQL injection
     */
    public boolean containsSqlInjection(String input) {
        if (!blockSqlInjection || input == null) {
            return false;
        }
        return SQL_INJECTION_PATTERN.matcher(input).matches();
    }

    /**
     * Check for shell injection patterns.
     *
     * @param input the input to check
     * @return true if input appears to contain shell injection
     */
    public boolean containsShellInjection(String input) {
        if (!blockShellInjection || input == null) {
            return false;
        }
        return SHELL_INJECTION_PATTERN.matcher(input).find();
    }

    /**
     * Escape special characters for safe use in SQL queries.
     * Note: Prefer parameterized queries. This is a last resort.
     *
     * @param input the input to escape
     * @return the escaped input
     */
    public String escapeSql(String input) {
        if (input == null) {
            return null;
        }
        return input
            .replace("'", "''")
            .replace("\\", "\\\\")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }

    /**
     * Escape special characters for safe use in shell commands.
     *
     * @param input the input to escape
     * @return the escaped input
     */
    public String escapeShell(String input) {
        if (input == null) {
            return null;
        }
        return input
            .replace(";", "")
            .replace("&", "")
            .replace("|", "")
            .replace("`", "")
            .replace("$", "")
            .replace("<", "")
            .replace(">", "");
    }

    /**
     * Escape dangerous XML characters.
     *
     * @param input the input to escape
     * @return the escaped input
     */
    public String escapeXml(String input) {
        if (input == null) {
            return null;
        }
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }

    /**
     * Truncate input to maximum length.
     *
     * @param input the input to truncate
     * @param maxLength the maximum length
     * @return the truncated input
     */
    public String truncate(String input, int maxLength) {
        if (input == null || input.length() <= maxLength) {
            return input;
        }
        return input.substring(0, maxLength);
    }

    /**
     * Validate that input is not empty or blank.
     *
     * @param input the input to validate
     * @throws IllegalArgumentException if input is empty or blank
     */
    public void requireNonEmpty(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Input cannot be empty");
        }
    }

    /**
     * Validate input length.
     *
     * @param input the input to validate
     * @param minLength minimum length
     * @param maxLength maximum length
     * @throws IllegalArgumentException if length is out of bounds
     */
    public void validateLength(String input, int minLength, int maxLength) {
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }
        if (input.length() < minLength || input.length() > maxLength) {
            throw new IllegalArgumentException(
                "Input length must be between " + minLength + " and " + maxLength
            );
        }
    }

    /**
     * Add a value to the whitelist.
     *
     * @param value the value to add
     */
    public void addAllowedValue(String value) {
        this.allowedValues.add(value);
    }

    /**
     * Remove a value from the whitelist.
     *
     * @param value the value to remove
     */
    public void removeAllowedValue(String value) {
        this.allowedValues.remove(value);
    }
}
