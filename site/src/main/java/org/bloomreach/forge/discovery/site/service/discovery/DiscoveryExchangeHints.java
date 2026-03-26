package org.bloomreach.forge.discovery.site.service.discovery;

import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.onehippo.cms7.crisp.api.exchange.ExchangeHint;
import org.onehippo.cms7.crisp.api.exchange.ExchangeHintBuilder;

final class DiscoveryExchangeHints {

    private static final String HEADER_AUTH_KEY = "auth-key";

    private DiscoveryExchangeHints() {
    }

    static ExchangeHint buildHint(ClientContext ctx) {
        ExchangeHintBuilder hintBuilder = ExchangeHintBuilder.create()
                .methodName("GET")
                .requestHeader("Content-Type", "application/json");
        if (isForwardClientHeaders()) {
            applyClientHeaders(hintBuilder, ctx);
        }
        return hintBuilder.build();
    }

    static ExchangeHint buildV2Hint(DiscoveryCredentials credentials, ClientContext ctx) {
        ExchangeHintBuilder hintBuilder = ExchangeHintBuilder.create()
                .methodName("GET")
                .requestHeader("Content-Type", "application/json");
        if (credentials.authKey() != null && !credentials.authKey().isBlank()) {
            hintBuilder.requestHeader(HEADER_AUTH_KEY, credentials.authKey());
        }
        if (isForwardClientHeaders()) {
            applyClientHeaders(hintBuilder, ctx);
        }
        return hintBuilder.build();
    }

    private static boolean isForwardClientHeaders() {
        return Boolean.parseBoolean(System.getProperty("brxdis.forwardClientHeaders", "true"));
    }

    private static void applyClientHeaders(ExchangeHintBuilder hintBuilder, ClientContext ctx) {
        if (ctx == null) {
            return;
        }
        if (ctx.userAgent() != null && !ctx.userAgent().isBlank()) {
            hintBuilder.requestHeader("User-Agent", ctx.userAgent());
        }
        if (ctx.acceptLanguage() != null && !ctx.acceptLanguage().isBlank()) {
            hintBuilder.requestHeader("Accept-Language", ctx.acceptLanguage());
        }
        if (ctx.xForwardedFor() != null && !ctx.xForwardedFor().isBlank()) {
            hintBuilder.requestHeader("X-Forwarded-For", ctx.xForwardedFor());
        }
    }
}
