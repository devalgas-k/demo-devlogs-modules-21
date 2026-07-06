package com.example.mcp.capabilities.tools.executor;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Interface pour les exécuteurs de tools MCP asynchrones.
 * <p>
 * Cette interface doit être implémentée par les tools qui effectuent
 * des opérations longues (appels API externes, requêtes de base de données,
 * traitements lourds, etc.).
 * </p>
 *
 * <p>Les tools implémentant cette interface permettent :</p>
 * <ul>
 *     <li>L'exécution non-bloquante du serveur MCP</li>
 *     <li>La gestion des timeouts coordonnés avec le LLM</li>
 *     <li>L'annulation propre si le LLM timeout</li>
 * </ul>
 *
 * <p>Exemple d'implémentation :</p>
 * <pre>{@code
 * @McpTool(
 *     name = "web_search",
 *     description = "Search the web for information",
 *     category = "utilities"
 * )
 * public class WebSearchTool implements AsyncMcpToolExecutor {
 *     
 *     private final SearchService searchService;
 *     private volatile boolean cancelled = false;
 *     
 *     @Override
 *     public CompletableFuture<ToolResponse> executeAsync(ToolRequest request) {
 *         return CompletableFuture.supplyAsync(() -> {
 *             String query = request.getParameters().get("query");
 *             
 *             // Check for cancellation
 *             if (cancelled) {
 *                 return ToolResponse.error(
 *                     McpError.TOOL_TIMEOUT.getCode(),
 *                     "Operation was cancelled"
 *                 );
 *             }
 *             
 *             try {
 *                 SearchResult result = searchService.search(query);
 *                 return ToolResponse.success(result);
 *             } catch (SearchException e) {
 *                 return ToolResponse.error(
 *                     McpError.SERVICE_UNAVAILABLE.getCode(),
 *                     "Search service unavailable: " + e.getMessage()
 *                 );
 *             }
 *         });
 *     }
 *     
 *     @Override
 *     public Duration getTimeout() {
 *         return Duration.ofSeconds(30);
 *     }
 *     
 *     @Override
 *     public void cancel() {
 *         this.cancelled = true;
 *     }
 * }
 * }</pre>
 *
 * @author MCP Server
 * @version 1.0.0
 * @see ToolExecutor
 */
public interface AsyncMcpToolExecutor {

    /**
     * Exécute le tool de manière asynchrone.
     * <p>
     * Pour les tools qui peuvent prendre plus de 5 secondes
     * (appels API, requêtes DB, etc.).
     * </p>
     *
     * @param request la requête contenant les paramètres du tool
     * @return un CompletableFuture contenant la réponse du tool
     */
    CompletableFuture<ToolResponse> executeAsync(ToolRequest request);

    /**
     * Retourne le timeout pour l'exécution asynchrone.
     * <p>
     * Par défaut, 60 secondes. Overridez cette méthode pour
     * des tools spécifiques nécessitant un timeout différent.
     * </p>
     *
     * @return la durée du timeout
     */
    default Duration getTimeout() {
        return Duration.ofSeconds(60);
    }

    /**
     * Annule l'exécution en cours.
     * <p>
     * Appelée lorsque le timeout défini par {@link #getTimeout()}
     * est dépassé. Implémentez cette méthode pour libérer proprement
     * les ressources (fermeture de connexions, annulation de requêtes, etc.).
     * </p>
     */
    default void cancel() {
        // Default: nothing to cancel
    }
}
