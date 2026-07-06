package com.example.mcp.capabilities.resources.loader;

import com.example.mcp.capabilities.resources.registry.ResourceRegistry.ResourceContent;

import java.net.URI;
import java.time.Duration;
import java.util.Set;

/**
 * Interface pour les chargeurs de resources MCP.
 * <p>
 * Les loaders sont responsables du chargement effectif des resources
 * à partir de différentes sources (filesystem, classpath, HTTP, etc.).
 * </p>
 *
 * <p>Chaque loader doit déclarer les schemes URI qu'il supporte
 * (ex: "file", "http", "classpath").</p>
 *
 * <p>Exemple d'implémentation pour un loader filesystem :</p>
 * <pre>{@code
 * @Component
 * public class FileSystemResourceLoader implements ResourceLoader {
 *     
 *     @Override
 *     public Set<String> getSupportedSchemes() {
 *         return Set.of("file");
 *     }
 *     
 *     @Override
 *     public ResourceContent load(URI uri) throws Exception {
 *         Path path = Paths.get(uri);
 *         byte[] data = Files.readAllBytes(path);
 *         String mimeType = Files.probeContentType(path);
 *         
 *         return new ResourceContent(
 *             data,
 *             mimeType != null ? mimeType : "application/octet-stream"
 *         );
 *     }
 *     
 *     @Override
 *     public Duration getCacheTtl() {
 *         return Duration.ofMinutes(5);
 *     }
 * }
 * }</pre>
 *
 * @author MCP Server
 * @version 1.0.0
 * @see ResourceRegistry
 */
public interface ResourceLoader {

    /**
     * Retourne les schemes URI supportés par ce loader.
     * <p>
     * Exemples de schemes :
     * <ul>
     *     <li>"file" - filesystem local</li>
     *     <li>"classpath" - resources du classpath</li>
     *     <li>"http" / "https" - resources web</li>
     *     <li>"s3" - Amazon S3</li>
     *     <li>"db" - resources de base de données</li>
     * </ul>
     *
     * @return un set des schemes supportés
     */
    Set<String> getSupportedSchemes();

    /**
     * Charge une resource depuis l'URI fourni.
     *
     * @param uri l'URI de la resource à charger
     * @return le contenu de la resource
     * @throws Exception si le chargement échoue
     */
    ResourceContent load(URI uri) throws Exception;

    /**
     * Retourne la durée de vie du cache pour ce loader.
     * <p>
     * Cette valeur est utilisée par le {@link com.example.mcp.capabilities.resources.registry.ResourceRegistry}
     * pour déterminer quand invalidate les resources cached.
     * </p>
     *
     * @return la durée de vie du cache
     */
    default Duration getCacheTtl() {
        return Duration.ofMinutes(10);
    }

    /**
     * Vérifie si ce loader peut charger l'URI fourni.
     *
     * @param uri l'URI à vérifier
     * @return true si ce loader peut charger l'URI
     */
    default boolean supports(URI uri) {
        if (uri == null || uri.getScheme() == null) {
            return false;
        }
        return getSupportedSchemes().contains(uri.getScheme().toLowerCase());
    }

    /**
     * Retourne la priorité du loader.
     * <p>
     * En cas de plusieurs loaders supportant le même scheme,
     * celui avec la priorité la plus haute est sélectionné.
     * </p>
     *
     * @return la priorité (plus haute = préférée)
     */
    default int getPriority() {
        return 0;
    }
}
