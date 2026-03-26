package org.bloomreach.forge.discovery.config;

import org.bloomreach.forge.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.config.model.DiscoverySettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.Optional;
import java.util.function.Function;

public class DiscoveryConfigReader {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryConfigReader.class);
    private static final CredentialSource ACCOUNT_ID = new CredentialSource(
            ConfigDefaults.ACCOUNT_ID_ENV, ConfigDefaults.ACCOUNT_ID_SYS, ConfigDefaults.ACCOUNT_ID_JCR);
    private static final CredentialSource DOMAIN_KEY = new CredentialSource(
            ConfigDefaults.DOMAIN_KEY_ENV, ConfigDefaults.DOMAIN_KEY_SYS, ConfigDefaults.DOMAIN_KEY_JCR);
    private static final CredentialSource API_KEY = new CredentialSource(
            ConfigDefaults.API_KEY_ENV, ConfigDefaults.API_KEY_SYS, ConfigDefaults.API_KEY_JCR);
    private static final CredentialSource AUTH_KEY = new CredentialSource(
            ConfigDefaults.AUTH_KEY_ENV, ConfigDefaults.AUTH_KEY_SYS, ConfigDefaults.AUTH_KEY_JCR);
    private static final CredentialSource ENVIRONMENT = new CredentialSource(
            ConfigDefaults.ENVIRONMENT_ENV, ConfigDefaults.ENVIRONMENT_SYS, ConfigDefaults.ENVIRONMENT_JCR);
    private static final IntSetting DEFAULT_PAGE_SIZE = new IntSetting(
            ConfigDefaults.DEFAULT_PAGE_SIZE_JCR, ConfigDefaults.DEFAULT_PAGE_SIZE);
    private static final StringSetting DEFAULT_SORT = new StringSetting(
            ConfigDefaults.DEFAULT_SORT_JCR, ConfigDefaults.DEFAULT_SORT);

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
        DiscoveryCredentials credentials = readCredentials(node);
        return DiscoveryConfig.of(credentials, readSettings(node, credentials.environment()));
    }

    /**
     * Resolves a credential field: env var → sys prop → JCR fallback.
     * Returns null if not found in any source (caller validates required fields).
     */
    String credential(Optional<Node> node, String envVar, String sysProp, String jcrProp)
            throws RepositoryException {
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
            if (fromJcr != null && !fromJcr.isBlank()) {
                log.debug("[credential] {} set (from JCR)", jcrProp);
                return fromJcr;
            }
            log.debug("[credential] {} JCR value is blank — skipping", jcrProp);
        }
        log.debug("[credential] {} not found in env/sys/JCR — returning null", jcrProp);
        return null;
    }

    String credential(Optional<Node> node, CredentialSource source) throws RepositoryException {
        return credential(node, source.envVar(), source.sysProp(), source.jcrProp());
    }

    /**
     * Returns raw env/sys credential values; {@code null} for any field not found.
     * No JCR, no coded defaults — structural fields are always {@code null}/{@code 0}.
     */
    public DiscoveryConfig credentialsFromEnvSys() {
        return DiscoveryConfig.of(credentialsFromEnvSysOnly(), new DiscoverySettings(null, null, null, 0, null));
    }

    public DiscoveryCredentials credentialsFromEnvSysOnly() {
        try {
            Optional<Node> noNode = Optional.empty();
            return new DiscoveryCredentials(
                    credential(noNode, ACCOUNT_ID),
                    credential(noNode, DOMAIN_KEY),
                    credential(noNode, API_KEY),
                    credential(noNode, AUTH_KEY),
                    credential(noNode, ENVIRONMENT)
            );
        } catch (RepositoryException e) {
            throw new IllegalStateException("Unexpected RepositoryException with no JCR node", e);
        }
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

    static String structural(Optional<Node> node, StringSetting setting) throws RepositoryException {
        return structural(node, setting.jcrProp(), setting.codedDefault());
    }

    static int structuralInt(Optional<Node> node, String jcrProp, int codedDefault)
            throws RepositoryException {
        if (node.isPresent() && node.get().hasProperty(jcrProp)) {
            return (int) node.get().getProperty(jcrProp).getLong();
        }
        return codedDefault;
    }

    static int structuralInt(Optional<Node> node, IntSetting setting) throws RepositoryException {
        return structuralInt(node, setting.jcrProp(), setting.codedDefault());
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private DiscoveryCredentials readCredentials(Optional<Node> node) throws RepositoryException {
        return new DiscoveryCredentials(
                credential(node, ACCOUNT_ID),
                credential(node, DOMAIN_KEY),
                credential(node, API_KEY),
                credential(node, AUTH_KEY),
                defaultIfBlank(credential(node, ENVIRONMENT), ConfigDefaults.ENVIRONMENT)
        );
    }

    private DiscoverySettings readSettings(Optional<Node> node, String environment) throws RepositoryException {
        if (ConfigDefaults.isStaging(environment)) {
            log.info("brxm-discovery: running against STAGING Discovery endpoints");
        }
        return new DiscoverySettings(
                ConfigDefaults.resolveBaseUri(structuralOrNull(node, ConfigDefaults.BASE_URI_JCR), environment),
                ConfigDefaults.resolvePathwaysBaseUri(structuralOrNull(node, ConfigDefaults.PATHWAYS_BASE_URI_JCR), environment),
                ConfigDefaults.resolveAutosuggestBaseUri(structuralOrNull(node, ConfigDefaults.AUTOSUGGEST_BASE_URI_JCR), environment),
                structuralInt(node, DEFAULT_PAGE_SIZE),
                structural(node, DEFAULT_SORT)
        );
    }

    static String structuralOrNull(Optional<Node> node, String jcrProp) throws RepositoryException {
        if (node.isPresent() && node.get().hasProperty(jcrProp)) {
            String value = node.get().getProperty(jcrProp).getString();
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    record CredentialSource(String envVar, String sysProp, String jcrProp) { }

    record StringSetting(String jcrProp, String codedDefault) { }

    record IntSetting(String jcrProp, int codedDefault) { }
}
