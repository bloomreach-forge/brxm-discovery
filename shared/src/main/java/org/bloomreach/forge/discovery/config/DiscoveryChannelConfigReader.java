package org.bloomreach.forge.discovery.config;

import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.function.Function;

/**
 * Resolves per-channel Discovery credential overrides.
 *
 * <p>Non-secret fields (accountId, domainKey) are stored directly as channel params.
 * Secret fields (apiKey, authKey) are stored as env-var names — resolved at runtime.
 */
public final class DiscoveryChannelConfigReader {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryChannelConfigReader.class);

    // Path suffix relative to the HST root node (e.g. /hst:myproject).
    // The root node name varies by project — we discover it dynamically.
    private static final String CHANNEL_INFO_SUFFIX = "hst:configurations/%s/hst:workspace/hst:channel/hst:channelinfo";

    public static final String PARAM_ACCOUNT_ID       = "discoveryAccountId";
    public static final String PARAM_DOMAIN_KEY       = "discoveryDomainKey";
    public static final String PARAM_API_KEY_ENV_VAR  = "discoveryApiKeyEnvVar";
    public static final String PARAM_AUTH_KEY_ENV_VAR = "discoveryAuthKeyEnvVar";

    private DiscoveryChannelConfigReader() {}

    /**
     * Called from site: values already resolved by the HST framework from hst:channelinfo.
     * Returns {@code null} when no non-blank override is found.
     */
    public static DiscoveryCredentials resolveOverrides(
            String accountId, String domainKey,
            String apiKeyEnvVar, String authKeyEnvVar) {
        return resolveOverrides(accountId, domainKey, apiKeyEnvVar, authKeyEnvVar, System::getenv);
    }

    /**
     * Overload with explicit env resolver — use to control env resolution in tests or
     * frameworks that manage environment variables differently.
     */
    public static DiscoveryCredentials resolveOverrides(
            String accountId, String domainKey,
            String apiKeyEnvVar, String authKeyEnvVar,
            Function<String, String> envResolver) {
        String acct   = blankToNull(accountId);
        String domain = blankToNull(domainKey);
        String api    = resolveEnvVar(apiKeyEnvVar, envResolver);
        String auth   = resolveEnvVar(authKeyEnvVar, envResolver);
        if (acct == null && domain == null
                && blankToNull(apiKeyEnvVar) == null && blankToNull(authKeyEnvVar) == null) {
            return null;
        }
        return new DiscoveryCredentials(acct, domain, api, auth, null);
    }

    /**
     * Called from CMS: reads the hst:channelinfo JCR node directly via the module session.
     * Returns {@code null} when channelId is blank, node absent, or no overrides are set.
     */
    public static DiscoveryCredentials resolveFromJcr(String channelId, Session session) {
        return resolveFromJcr(channelId, session, System::getenv);
    }

    /**
     * Overload with explicit env resolver — use to control env resolution in tests or frameworks
     * that manage environment variables differently.
     */
    public static DiscoveryCredentials resolveFromJcr(String channelId, Session session,
                                                      Function<String, String> envResolver) {
        if (channelId == null || channelId.isBlank()) {
            return null;
        }
        log.debug("[resolveFromJcr] channelId='{}'", channelId);
        try {
            Node channelInfo = findChannelInfoNode(channelId, session);
            if (channelInfo == null) {
                log.debug("[resolveFromJcr] channelinfo node not found for channelId='{}'", channelId);
                return null;
            }
            log.debug("[resolveFromJcr] channelinfo node found at '{}', reading overrides", channelInfo.getPath());
            DiscoveryCredentials result = resolveOverrides(
                    stringProp(channelInfo, PARAM_ACCOUNT_ID),
                    stringProp(channelInfo, PARAM_DOMAIN_KEY),
                    stringProp(channelInfo, PARAM_API_KEY_ENV_VAR),
                    stringProp(channelInfo, PARAM_AUTH_KEY_ENV_VAR),
                    envResolver);
            log.debug("[resolveFromJcr] overrides for channelId='{}': {}", channelId,
                    result != null ? "accountId=" + result.accountId() + " domainKey=" + result.domainKey() : "none");
            return result;
        } catch (RepositoryException e) {
            log.warn("brxm-discovery: Cannot read channel info for '{}': {}", channelId, e.getMessage());
            return null;
        }
    }

    /**
     * Searches all {@code hst:*} root nodes for the channelinfo node for the given channelId.
     * This handles projects where the HST root is named {@code hst:myproject} rather than the
     * legacy {@code hst:hst}.
     */
    private static Node findChannelInfoNode(String channelId, Session session) throws RepositoryException {
        String suffix = String.format(CHANNEL_INFO_SUFFIX, channelId);
        NodeIterator hstRoots = session.getRootNode().getNodes("hst:*");
        while (hstRoots.hasNext()) {
            Node hstRoot = hstRoots.nextNode();
            String path = hstRoot.getPath() + "/" + suffix;
            if (session.nodeExists(path)) {
                return session.getNode(path);
            }
        }
        return null;
    }

    private static String stringProp(Node node, String name) throws RepositoryException {
        return node.hasProperty(name) ? node.getProperty(name).getString() : null;
    }

    private static String resolveEnvVar(String varName, Function<String, String> envResolver) {
        if (varName == null || varName.isBlank()) {
            return null;
        }
        String value = envResolver.apply(varName.trim());
        return (value == null || value.isBlank()) ? null : value;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
