package org.bloomreach.forge.discovery.site.service.discovery.config;

import com.google.common.base.CaseFormat;
import org.bloomreach.forge.discovery.site.service.discovery.config.model.DiscoveryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DiscoveryConfigReader {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryConfigReader.class);
    private final Function<String, String> envLookup;

    public DiscoveryConfigReader() {
        this(System::getenv);
    }

    /** Package-private for testing — pass {@code ignored -> null} to suppress env var lookup. */
    DiscoveryConfigReader(Function<String, String> envLookup) {
        this.envLookup = envLookup;
    }

    /**
     * Reads config from a JCR node. All structural fields fall back to coded defaults
     * if the JCR property is absent.
     */
    public DiscoveryConfig read(Node configNode) throws RepositoryException {
        return readFromNode(Optional.of(configNode));
    }

    /**
     * Builds config using only env/sys credentials and coded defaults for structural fields.
     * No JCR node required.
     */
    public DiscoveryConfig readWithDefaults() {
        try {
            return readFromNode(Optional.empty());
        } catch (RepositoryException e) {
            // Cannot happen with empty node, but satisfy the compiler
            throw new IllegalStateException("Unexpected RepositoryException with no JCR node", e);
        }
    }

    DiscoveryConfig readFromNode(Optional<Node> node) throws RepositoryException {
        // Tier 1: Credentials — env var → sys prop → JCR fallback
        String accountId  = credential(node, "brxdis:accountId",  "brxdis.accountId");
        String domainKey  = credential(node, "brxdis:domainKey",  "brxdis.domainKey");
        String apiKey     = credential(node, "brxdis:apiKey",     "brxdis.apiKey");
        String authKey    = credential(node, "brxdis:authKey",    "brxdis.authKey");
        String environment = credential(node, "brxdis:environment", "brxdis.environment");

        if (environment == null || environment.isBlank()) {
            environment = ConfigDefaults.ENVIRONMENT;
        }

        // Tier 2: Structural — JCR → coded default
        String baseUri         = structural(node, "brxdis:baseUri",         ConfigDefaults.BASE_URI);
        String pathwaysBaseUri = structural(node, "brxdis:pathwaysBaseUri", ConfigDefaults.PATHWAYS_BASE_URI);
        int defaultPageSize    = structuralInt(node, "brxdis:defaultPageSize", ConfigDefaults.DEFAULT_PAGE_SIZE);
        String defaultSort     = structural(node, "brxdis:defaultSort",     ConfigDefaults.DEFAULT_SORT);

        return new DiscoveryConfig(accountId, domainKey, apiKey, authKey,
                baseUri, pathwaysBaseUri, environment, defaultPageSize, defaultSort);
    }

    /**
     * Resolves a credential field: env var → sys prop → JCR fallback.
     * Returns null if not found in any source (caller validates required fields).
     */
    String credential(Optional<Node> node, String jcrProp, String sysProp)
            throws RepositoryException {
        String envVar = Arrays.stream(sysProp.split("\\."))
                .map(part -> CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, part))
                .collect(Collectors.joining("_"));
        String fromEnv = envLookup.apply(envVar);
        if (fromEnv != null && !fromEnv.isBlank()) {
            log.debug("[credential] {}={} (from env var {})", jcrProp, "set", envVar);
            return fromEnv;
        }
        String fromSys = System.getProperty(sysProp);
        if (fromSys != null && !fromSys.isBlank()) {
            log.debug("[credential] {}={} (from sys prop {})", jcrProp, "set", sysProp);
            return fromSys;
        }
        if (node.isPresent() && node.get().hasProperty(jcrProp)) {
            String fromJcr = node.get().getProperty(jcrProp).getString();
            log.debug("[credential] {}='{}' (from JCR; blank={})", jcrProp,
                    fromJcr != null && !fromJcr.isBlank() ? "set" : fromJcr, fromJcr == null || fromJcr.isBlank());
            return fromJcr;
        }
        log.debug("[credential] {} not found in env/sys/JCR — returning null", jcrProp);
        return null;
    }

    /**
     * Resolves a structural config field: JCR → coded default.
     */
    static String structural(Optional<Node> node, String jcrProp, String codedDefault)
            throws RepositoryException {
        if (node.isPresent() && node.get().hasProperty(jcrProp)) {
            String fromJcr = node.get().getProperty(jcrProp).getString();
            if (fromJcr != null && !fromJcr.isBlank()) {
                return fromJcr;
            }
        }
        return codedDefault;
    }

    static int structuralInt(Optional<Node> node, String jcrProp, int codedDefault)
            throws RepositoryException {
        if (node.isPresent() && node.get().hasProperty(jcrProp)) {
            return (int) node.get().getProperty(jcrProp).getLong();
        }
        return codedDefault;
    }
}
