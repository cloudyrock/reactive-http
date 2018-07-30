package com.github.cloudyrock.reactivehttp;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class DimmerMethodMetadata {

    private final String url;
    private final MediaType contentType;
    private final Class parameterizedType;
    private final HttpMethod httpMethod;
    private final List<ParameterMetadata> parametersMetadata;
    private final Map<String, Set<String>> defaultHeaders;
    private final BodyMapperObject bodyEncoder;
    private final String dimmerFeature;

    DimmerMethodMetadata(HttpMethod httpMethod,
                         String url,
                         MediaType contentType,
                         Class parameterizedType,
                         List<ParameterMetadata> parametersMetadata,
                         Map<String, Set<String>> defaultHeaders,
                         BodyMapperObject bodyEncoder,
                         String dimmerFeature) {
        this.url = url;
        this.parameterizedType = parameterizedType;
        this.httpMethod = httpMethod;
        this.contentType = contentType;
        this.parametersMetadata = parametersMetadata;
        this.defaultHeaders = defaultHeaders;
        this.bodyEncoder = bodyEncoder;
        this.dimmerFeature = dimmerFeature;
    }

    String getUrl() {
        return url;
    }

    Class getParameterizedType() {
        return parameterizedType;
    }

    HttpMethod getHttpMethod() {
        return httpMethod;
    }

    MediaType getContentType() {
        return contentType;
    }

    List<ParameterMetadata> getParametersMetadata() {
        return parametersMetadata;
    }

    Map<String, Set<String>> getDefaultHeaders() {
        return defaultHeaders;
    }

    Optional<BodyMapperObject> getBodyEncoder() {
        return Optional.ofNullable(bodyEncoder);
    }

    Optional<String> getDimmerFeature() {
        return Optional.ofNullable(dimmerFeature);
    }
}
