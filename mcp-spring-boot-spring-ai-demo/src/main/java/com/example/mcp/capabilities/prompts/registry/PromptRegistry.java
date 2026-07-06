package com.example.mcp.capabilities.prompts.registry;

import com.example.mcp.capabilities.prompts.template.PromptTemplate;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Central registry for MCP prompt templates.
 * Manages registration, discovery, and resolution of MCP prompt templates.
 *
 * <p>Features:</p>
 * <ul>
 *     <li>Manual template registration</li>
 *     <li>Automatic discovery from classpath (.st files)</li>
 *     <li>Parameterized template support</li>
 *     <li>Thread-safety via ConcurrentHashMap</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * @Autowired
 * private PromptRegistry promptRegistry;
 * 
 * // Resolve a prompt with parameters
 * String resolved = promptRegistry.resolve("code_review", 
 *     Map.of("repository", "my-app", "branch", "main"));
 * 
 * // List all prompts
 * List<PromptEntry> prompts = promptRegistry.getAllPrompts();
 * }</pre>
 */
@Component
public class PromptRegistry {

    private static final Logger log = LoggerFactory.getLogger(PromptRegistry.class);
    private static final String DEFAULT_PROMPT_PATTERN = "classpath:/mcp/prompts/*.st";

    private final ApplicationContext applicationContext;
    private final ConcurrentHashMap<String, PromptEntry> prompts = new ConcurrentHashMap<>();

    private final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

    // Constructor with Spring context injection
    public PromptRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Registry initialization with automatic prompt discovery.
     */
    @PostConstruct
    public void initialize() {
        log.info("Initializing PromptRegistry...");
        
        // Discover PromptTemplate beans
        Map<String, PromptTemplate> templateBeans = applicationContext.getBeansOfType(PromptTemplate.class);
        templateBeans.forEach((beanName, template) -> {
            register(new PromptEntry(
                template.getName(),
                template.getDescription(),
                template.getCategory(),
                template
            ));
        });

        // Load prompts from classpath
        loadPromptsFromClasspath();

        log.info("PromptRegistry initialized with {} prompts", prompts.size());
    }

    /**
     * Load prompts from classpath.
     */
    private void loadPromptsFromClasspath() {
        try {
            Resource[] resources = resourcePatternResolver.getResources(DEFAULT_PROMPT_PATTERN);
            
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) continue;

                String name = filename.replace(".st", "");
                String content = loadResourceContent(resource);
                
                // Create a simple template from the file content
                SimplePromptTemplate template = new SimplePromptTemplate(name, content);
                register(new PromptEntry(
                    name,
                    "Prompt loaded from classpath: " + filename,
                    "discovered",
                    template
                ));
                
                log.debug("Loaded prompt from classpath: {}", filename);
            }
        } catch (IOException e) {
            log.debug("No prompts found in classpath at pattern: {}", DEFAULT_PROMPT_PATTERN);
        }
    }

    /**
     * Load resource content.
     */
    private String loadResourceContent(Resource resource) throws IOException {
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Register a prompt in the registry.
     *
     * @param entry the prompt entry to register
     */
    public void register(PromptEntry entry) {
        if (prompts.containsKey(entry.name())) {
            log.warn("Prompt {} is already registered. Replacing.", entry.name());
        }
        prompts.put(entry.name(), entry);
        log.debug("Registered prompt: {} (category={})", entry.name(), entry.category());
    }

    /**
     * Register a prompt with text content.
     *
     * @param name the prompt name
     * @param description the prompt description
     * @param content the template content
     * @param category the prompt category
     */
    public void register(String name, String description, String content, String category) {
        register(new PromptEntry(name, description, category, new SimplePromptTemplate(name, content)));
    }

    /**
     * Resolve a prompt with parameters.
     *
     * @param name the prompt name
     * @param parameters the substitution parameters
     * @return the resolved prompt
     * @throws IllegalArgumentException if prompt does not exist
     */
    public String resolve(String name, Map<String, Object> parameters) {
        PromptEntry entry = prompts.get(name);
        if (entry == null) {
            throw new IllegalArgumentException("Prompt not found: " + name);
        }

        try {
            return entry.template().render(parameters);
        } catch (Exception e) {
            log.error("Failed to resolve prompt {}: {}", name, e.getMessage());
            throw new IllegalArgumentException("Failed to resolve prompt: " + name, e);
        }
    }

    /**
     * Resolve a prompt without parameters.
     *
     * @param name the prompt name
     * @return the resolved prompt
     */
    public String resolve(String name) {
        return resolve(name, Map.of());
    }

    /**
     * Get a prompt by name.
     *
     * @param name the prompt name
     * @return an Optional containing the entry if found
     */
    public Optional<PromptEntry> getPrompt(String name) {
        return Optional.ofNullable(prompts.get(name));
    }

    /**
     * Get all registered prompts.
     *
     * @return a collection of all prompt entries
     */
    public Collection<PromptEntry> getAllPrompts() {
        return prompts.values();
    }

    /**
     * Get prompts by category.
     *
     * @param category the category to search
     * @return a list of prompts in the category
     */
    public List<PromptEntry> getByCategory(String category) {
        return prompts.values().stream()
            .filter(entry -> entry.category().equalsIgnoreCase(category))
            .collect(Collectors.toList());
    }

    /**
     * Filter prompts according to a predicate.
     *
     * @param predicate the filter predicate
     * @return a list of matching prompts
     */
    public List<PromptEntry> findPrompts(Predicate<PromptEntry> predicate) {
        return prompts.values().stream()
            .filter(predicate)
            .collect(Collectors.toList());
    }

    /**
     * Get names of all available categories.
     *
     * @return a collection of category names
     */
    public Collection<String> getCategories() {
        return prompts.values().stream()
            .map(PromptEntry::category)
            .collect(Collectors.toSet());
    }

    /**
     * Remove a prompt from the registry.
     *
     * @param name the name of the prompt to remove
     * @return true if the prompt was removed
     */
    public boolean unregister(String name) {
        return prompts.remove(name) != null;
    }

    /**
     * Check if a prompt exists.
     *
     * @param name the prompt name
     * @return true if the prompt exists
     */
    public boolean hasPrompt(String name) {
        return prompts.containsKey(name);
    }

    /**
     * Return the number of registered prompts.
     *
     * @return the prompt count
     */
    public int getPromptCount() {
        return prompts.size();
    }

    /**
     * Return the expected variables for a prompt.
     *
     * @param name the prompt name
     * @return a set of variable names
     */
    public java.util.Set<String> getPromptVariables(String name) {
        PromptEntry entry = prompts.get(name);
        if (entry == null) {
            throw new IllegalArgumentException("Prompt not found: " + name);
        }
        return entry.template().getVariableNames();
    }

    /**
     * Entry record representing a registered prompt.
     *
     * @param name the unique prompt name
     * @param description the prompt description
     * @param category the prompt category
     * @param template the prompt template
     */
    public record PromptEntry(
        String name,
        String description,
        String category,
        PromptTemplate template
    ) {
    }

    /**
     * Simple implementation of PromptTemplate for static content.
     */
    private static class SimplePromptTemplate implements PromptTemplate {

        private final String name;
        private final String content;

        public SimplePromptTemplate(String name, String content) {
            this.name = name;
            this.content = content;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return "Simple template for: " + name;
        }

        @Override
        public String render(Map<String, Object> variables) {
            String result = content;
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                result = result.replace(placeholder, value);
            }
            return result;
        }
    }
}
