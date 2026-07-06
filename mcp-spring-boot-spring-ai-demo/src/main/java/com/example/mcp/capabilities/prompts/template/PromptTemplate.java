package com.example.mcp.capabilities.prompts.template;

import java.util.Map;
import java.util.Set;

/**
 * Interface pour les templates de prompts MCP.
 * <p>
 * Un template de prompt définit un pattern de prompt réutilisable
 * avec des variables paramétrables. Les templates permettent :
 * </p>
 * <ul>
 *     <li>La réutilisation de prompts complexes</li>
 *     <li>La paramétrisation dynamique du contenu</li>
 *     <li>La séparation entre structure et données</li>
 * </ul>
 *
 * <p>Exemple d'implémentation avec StringTemplate :</p>
 * <pre>{@code
 * @Component
 * public class CodeReviewPrompt implements PromptTemplate {
 *     
 *     private static final String TEMPLATE = """
 *         Please review the code in repository {{repository}}
 *         on branch {{branch}}.
 *         
 *         Focus on:
 *         - {{focus_areas}}
 *         
 *         Provide a detailed analysis with:
 *         1. Code quality issues
 *         2. Security vulnerabilities
 *         3. Performance concerns
 *         """;
 *     
 *     @Override
 *     public String getName() {
 *         return "code_review";
 *     }
 *     
 *     @Override
 *     public String getDescription() {
 *         return "Generate a code review for a repository";
 *     }
 *     
 *     @Override
 *     public String render(Map<String, Object> variables) {
 *         return TEMPLATE
 *             .replace("{{repository}}", String.valueOf(variables.get("repository")))
 *             .replace("{{branch}}", String.valueOf(variables.get("branch")))
 *             .replace("{{focus_areas}}", String.valueOf(variables.get("focus_areas", "all areas")));
 *     }
 *     
 *     @Override
 *     public Set<String> getVariableNames() {
 *         return Set.of("repository", "branch", "focus_areas");
 *     }
 * }
 * }</pre>
 *
 * <p>Exemple d'utilisation :</p>
 * <pre>{@code
 * @Autowired
 * private PromptRegistry promptRegistry;
 * 
 * String prompt = promptRegistry.resolve("code_review", Map.of(
 *     "repository", "my-app",
 *     "branch", "main",
 *     "focus_areas", "security, performance"
 * ));
 * 
 * ChatClient.message(prompt).call();
 * }</pre>
 *
 * @author MCP Server
 * @version 1.0.0
 * @see PromptRegistry
 */
public interface PromptTemplate {

    /**
     * Retourne le nom unique du template.
     * <p>
     * Ce nom est utilisé pour identifier le template
     * dans le registre et lors des appels à {@link PromptRegistry#resolve}.
     * </p>
     *
     * @return le nom du template
     */
    String getName();

    /**
     * Retourne la description du template.
     * <p>
     * La description est utilisée par le LLM pour comprendre
     * quand et comment utiliser ce template.
     * </p>
     *
     * @return la description du template
     */
    String getDescription();

    /**
     * Retourne la catégorie du template.
     * <p>
     * La catégorie permet de regrouper les templates
     * (ex: "code_review", "documentation", "analysis").
     * </p>
     *
     * @return la catégorie du template
     */
    default String getCategory() {
        return "general";
    }

    /**
     * Rend le template avec les variables fournies.
     * <p>
     * Cette méthode substitue les placeholders du template
     * par les valeurs des variables.
     * </p>
     *
     * <p>Format des placeholders : {@code {{variableName}}}</p>
     *
     * @param variables les variables de substitution
     * @return le prompt rendu
     * @throws IllegalArgumentException si une variable requise est manquante
     */
    String render(Map<String, Object> variables);

    /**
     * Retourne les noms des variables attendues par ce template.
     * <p>
     * Cette information est utilisée pour la validation
     * et pour informer le LLM des paramètres disponibles.
     * </p>
     *
     * @return un set des noms de variables
     */
    default Set<String> getVariableNames() {
        return Set.of();
    }

    /**
     * Retourne les variables requises (sans valeur par défaut).
     *
     * @return un set des noms de variables requises
     */
    default Set<String> getRequiredVariables() {
        return getVariableNames();
    }

    /**
     * Vérifie si une variable est requise.
     *
     * @param variableName le nom de la variable
     * @return true si la variable est requise
     */
    default boolean isRequired(String variableName) {
        return getRequiredVariables().contains(variableName);
    }

    /**
     * Valide les variables fournies contre les attentes du template.
     *
     * @param variables les variables à valider
     * @throws IllegalArgumentException si une variable requise est manquante
     */
    default void validateVariables(Map<String, Object> variables) {
        for (String required : getRequiredVariables()) {
            if (!variables.containsKey(required) || variables.get(required) == null) {
                throw new IllegalArgumentException(
                    "Required variable '" + required + "' is missing for template: " + getName()
                );
            }
        }
    }

    /**
     * Rend le template avec validation des variables.
     *
     * @param variables les variables de substitution
     * @param strict si true, lance une exception pour les variables inconnues
     * @return le prompt rendu
     */
    default String render(Map<String, Object> variables, boolean strict) {
        if (strict) {
            validateVariables(variables);

            // Check for unknown variables
            for (String key : variables.keySet()) {
                if (!getVariableNames().contains(key)) {
                    throw new IllegalArgumentException(
                        "Unknown variable '" + key + "' for template: " + getName()
                    );
                }
            }
        }
        return render(variables);
    }
}
