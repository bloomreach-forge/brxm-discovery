package org.bloomreach.forge.discovery.request;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public record DiscoveryRequestSpec(String path, List<QueryParameter> queryParameters) {

    public DiscoveryRequestSpec {
        Objects.requireNonNull(path, "path must not be null");
        queryParameters = List.copyOf(queryParameters);
    }

    public void forEachQueryParameter(BiConsumer<String, String> consumer) {
        queryParameters.forEach(parameter -> consumer.accept(parameter.name(), parameter.value()));
    }

    public static Builder builder(String path) {
        return new Builder(path);
    }

    public record QueryParameter(String name, String value) {
        public QueryParameter {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(value, "value must not be null");
        }
    }

    public static final class Builder {
        private final String path;
        private final List<QueryParameter> queryParameters = new ArrayList<>();

        private Builder(String path) {
            this.path = path;
        }

        public Builder queryParam(String name, Object value) {
            if (value != null) {
                queryParameters.add(new QueryParameter(name, String.valueOf(value)));
            }
            return this;
        }

        public Builder queryParamIfNotBlank(String name, String value) {
            if (value != null && !value.isBlank()) {
                queryParameters.add(new QueryParameter(name, value));
            }
            return this;
        }

        public DiscoveryRequestSpec build() {
            return new DiscoveryRequestSpec(path, queryParameters);
        }
    }
}
