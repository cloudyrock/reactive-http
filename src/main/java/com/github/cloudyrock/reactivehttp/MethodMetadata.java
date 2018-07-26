package com.github.cloudyrock.reactivehttp;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class MethodMetadata {

    private final String url;
    private final MediaType contentType;
    private final Class parameterizedType;
    private final HttpMethod httpMethod;
    private final List<ParameterMetadata> parametersMetadata;
    private final Map<String, Set<String>> defaultHeaders;
    private final BodyMapperObject bodyEncoder;

    MethodMetadata(HttpMethod httpMethod,
                   String url,
                   MediaType contentType,
                   Class parameterizedType,
                   List<ParameterMetadata> parametersMetadata,
                   Map<String, Set<String>> defaultHeaders,
                   BodyMapperObject bodyEncoder) {
        this.url = url;
        this.parameterizedType = parameterizedType;
        this.httpMethod = httpMethod;
        this.contentType = contentType;
        this.parametersMetadata = parametersMetadata;
        this.defaultHeaders = defaultHeaders;
        this.bodyEncoder = bodyEncoder;
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
}
