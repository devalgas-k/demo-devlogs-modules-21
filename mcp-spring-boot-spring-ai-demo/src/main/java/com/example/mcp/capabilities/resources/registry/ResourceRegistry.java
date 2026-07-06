package com.example.mcp.capabilities.resources.registry;

import com.example.mcp.capabilities.resources.loader.ResourceLoader;
import com.example.mcp.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Registre centralisé pour les resources MCP.
 * <p>
 * Ce composant gère l'enregistrement, la découverte et le chargement des resources MCP.
 * Les resources sont identifiées par des URI (ex: {@code file:///config/app.yml}).
 * </p>
 *
 * <p>Fonctionnalités :</p>
 * <ul>
 *     <li>Enregistrement de loaders par scheme (file, http, classpath, etc.)</li>
 *     <li>Chargement thread-safe des resources</li>
 *     <li>Découverte automatique des loaders via component scanning</li>
 *     <li>Cache optionnel des resources chargées</li>
 * </ul>
 *
 * <p>Exemple d'utilisation :</p>
 * <pre>{@code
 * @Autowired
 * private ResourceRegistry resourceRegistry;
 * 
 * // Charger une resource
 * ResourceContent content = resourceRegistry.load("file:///data/config.json");
 * 
 * // Lister les resources par scheme
 * List<ResourceEntry> fileResources = resourceRegistry.getByScheme("file");
 * }</pre>
 *
 * @author MCP Server
 * @version 1.0.0
 * @see ResourceLoader
 */
@Component
public class ResourceRegistry {

    private static final Logger log = LoggerFactory.getLogger(ResourceRegistry.class);

    private final ApplicationContext applicationContext;
    private final ConcurrentHashMap<String, ResourceEntry> resources = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ResourceLoader> loadersByScheme = new ConcurrentHashMap<>();

    @Value("${mcp.resource.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${mcp.resource.cache.max-size:100}")
    private int maxCacheSize;

    /**
     * Cache des resources chargées.
     */
    private final ConcurrentHashMap<String, CachedResource> resourceCache = new ConcurrentHashMap<>();

    /**
     * Constructeur avec injection du contexte Spring.
     *
     * @param applicationContext le contexte Spring
     */
    public ResourceRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Initialisation du registre via component scanning.
     */
    @PostConstruct
    public void initialize() {
        log.info("Initializing ResourceRegistry...");

        // Discover all ResourceLoader beans
        Map<String, ResourceLoader> loaders = applicationContext.getBeansOfType(ResourceLoader.class);

        loaders.forEach((beanName, loader) -> {
            loader.getSupportedSchemes().forEach(scheme -> {
                loadersByScheme.put(scheme.toLowerCase(), loader);
                log.debug("Registered ResourceLoader for scheme: {} -> {}",
                    scheme, loader.getClass().getSimpleName());
            });
        });

        log.info("ResourceRegistry initialized with {} scheme loaders: {}",
            loadersByScheme.size(), loadersByScheme.keySet());
    }

    /**
     * Enregistre une resource statique.
     *
     * @param uri l'URI de la resource
     * @param description la description de la resource
     * @param mimeType le type MIME de la resource
     */
    public void register(String uri, String description, String mimeType) {
        URI parsedUri = URI.create(uri);
        String scheme = parsedUri.getScheme().toLowerCase();

        resources.put(uri, new ResourceEntry(
            uri,
            description,
            mimeType,
            scheme,
            null
        ));

        log.debug("Registered static resource: {} (scheme={}, mimeType={})",
            uri, scheme, mimeType);
    }

    /**
     * Enregistre un loader pour un scheme.
     *
     * @param scheme le scheme (ex: "file", "http", "classpath")
     * @param loader le loader à utiliser
     */
    public void registerLoader(String scheme, ResourceLoader loader) {
        loadersByScheme.put(scheme.toLowerCase(), loader);
        log.debug("Registered custom ResourceLoader for scheme: {} -> {}",
            scheme, loader.getClass().getSimpleName());
    }

    /**
     * Charge une resource par son URI.
     *
     * @param uri l'URI de la resource
     * @return le contenu de la resource
     * @throws ResourceNotFoundException si la resource n'existe pas
     * @throws IllegalArgumentException si le scheme n'est pas supporté
     */
    public ResourceContent load(String uri) {
        // Check cache first
        if (cacheEnabled) {
            CachedResource cached = resourceCache.get(uri);
            if (cached != null && !cached.isExpired()) {
                log.debug("Resource loaded from cache: {}", uri);
                return cached.content();
            }
        }

        URI parsedUri = URI.create(uri);
        String scheme = parsedUri.getScheme().toLowerCase();

        ResourceLoader loader = loadersByScheme.get(scheme);
        if (loader == null) {
            throw new IllegalArgumentException(
                "Unsupported URI scheme: " + scheme + ". Supported: " + loadersByScheme.keySet()
            );
        }

        try {
            ResourceContent content = loader.load(parsedUri);

            // Cache the result
            if (cacheEnabled && content != null) {
                cacheResource(uri, content, loader.getCacheTtl());
            }

            return content;
        } catch (Exception e) {
            log.error("Failed to load resource: {} - {}", uri, e.getMessage());
            throw new ResourceNotFoundException("Resource not found or inaccessible: " + uri, e);
        }
    }

    /**
     * Charge une resource de manière asynchrone.
     *
     * @param uri l'URI de la resource
     * @return un CompletableFuture contenant le contenu
     */
    public java.util.concurrent.CompletableFuture<ResourceContent> loadAsync(String uri) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> load(uri));
    }

    /**
     * Récupère une resource par son URI.
     *
     * @param uri l'URI de la resource
     * @return un Optional contenant l'entrée si trouvée
     */
    public Optional<ResourceEntry> getResource(String uri) {
        return Optional.ofNullable(resources.get(uri));
    }

    /**
     * Récupère toutes les resources enregistrées.
     *
     * @return une collection de toutes les resources
     */
    public Collection<ResourceEntry> getAllResources() {
        return resources.values();
    }

    /**
     * Récupère les resources par scheme.
     *
     * @param scheme le scheme à filtrer
     * @return une liste des resources du scheme
     */
    public List<ResourceEntry> getByScheme(String scheme) {
        String normalizedScheme = scheme.toLowerCase();
        return resources.values().stream()
            .filter(entry -> entry.scheme().equals(normalizedScheme))
            .collect(Collectors.toList());
    }

    /**
     * Filtre les resources selon un prédicat.
     *
     * @param predicate le prédicat de filtrage
     * @return une liste des resources correspondantes
     */
    public List<ResourceEntry> findResources(Predicate<ResourceEntry> predicate) {
        return resources.values().stream()
            .filter(predicate)
            .collect(Collectors.toList());
    }

    /**
     * Supprime une resource du registre.
     *
     * @param uri l'URI de la resource à supprimer
     * @return true si la resource a été supprimée
     */
    public boolean unregister(String uri) {
        resourceCache.remove(uri);
        return resources.remove(uri) != null;
    }

    /**
     * Invalide le cache pour une resource.
     *
     * @param uri l'URI de la resource à invalidate
     */
    public void invalidateCache(String uri) {
        resourceCache.remove(uri);
        log.debug("Cache invalidated for resource: {}", uri);
    }

    /**
     * Vide le cache completely.
     */
    public void clearCache() {
        resourceCache.clear();
        log.info("Resource cache cleared");
    }

    /**
     * Met à jour le cache avec une nouvelle resource.
     */
    private void cacheResource(String uri, ResourceContent content, java.time.Duration ttl) {
        if (resourceCache.size() >= maxCacheSize) {
            // Remove oldest entry
            String oldestKey = resourceCache.keys().nextElement();
            resourceCache.remove(oldestKey);
        }
        resourceCache.put(uri, new CachedResource(content, ttl));
    }

    /**
     * Retourne le nombre de resources enregistrées.
     */
    public int getResourceCount() {
        return resources.size();
    }

    /**
     * Retourne le nombre de schemes supportés.
     */
    public int getSchemeCount() {
        return loadersByScheme.size();
    }

    /**
     * Entry record representing a registered resource.
     *
     * @param uri l'URI de la resource
     * @param description la description de la resource
     * @param mimeType le type MIME
     * @param scheme le scheme URI
     * @param loader le loader optionnel pour les resources dynamiques
     */
    public record ResourceEntry(
        String uri,
        String description,
        String mimeType,
        String scheme,
        ResourceLoader loader
    ) {
        public boolean isDynamic() {
            return loader != null;
        }
    }

    /**
     * Record representing cached resource content.
     *
     * @param content le contenu cached
     * @param ttl la durée de vie du cache
     */
    private record CachedResource(ResourceContent content, java.time.Duration ttl) {
        boolean isExpired() {
            return java.time.Duration.between(content.loadedAt(), java.time.Instant.now()).compareTo(ttl) > 0;
        }
    }

    /**
     * Record representing loaded resource content.
     *
     * @param data les données de la resource
     * @param mimeType le type MIME
     * @param metadata métadonnées additionnelles
     * @param loadedAt timestamp de chargement
     */
    public record ResourceContent(
        byte[] data,
        String mimeType,
        Map<String, String> metadata,
        java.time.Instant loadedAt
    ) {
        public ResourceContent(byte[] data, String mimeType) {
            this(data, mimeType, Map.of(), java.time.Instant.now());
        }

        public String asText() {
            return new String(data);
        }
    }
}
