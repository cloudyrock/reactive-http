package org.github.cloudyrock.reactivehttp;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.List;

final class MethodMetadata {

    private final String url;
    private final MediaType contentType;
    private final Class parameterizedType;
    private final HttpMethod method;
    private final List<ParameterMetadata> parametersMetadata;

    MethodMetadata(HttpMethod method,
                   String url,
                   MediaType contentType,
                   Class parameterizedType,
                   List<ParameterMetadata> parametersMetadata) {
        this.url = url;
        this.parameterizedType = parameterizedType;
        this.method = method;
        this.contentType = contentType;
        this.parametersMetadata = parametersMetadata;
    }

    String getUrl() {
        return url;
    }

    Class getParameterizedType() {
        return parameterizedType;
    }

    HttpMethod getMethod() {
        return method;
    }

    MediaType getContentType() {
        return contentType;
    }

    List<ParameterMetadata> getParametersMetadata() {
        return parametersMetadata;
    }
}
