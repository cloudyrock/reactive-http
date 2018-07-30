package com.github.cloudyrock.reactivehttp;

import com.github.cloudyrock.reactivehttp.exception.ReactiveHttpRuntimeException;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

class ReactiveHttpInterceptor implements MethodInterceptor {

    private static final Supplier<BodyMapperObject> defaultBodyEncoder = () -> body -> body;

    private final WebClient client;
    private final Map<Method, MethodMetadata> metadataMap;
    private final Map<Class, Function<?, String>> defaultParamTypeEncoders;

    ReactiveHttpInterceptor(WebClient client,
                            Map<Method, MethodMetadata> metadataMap,
                            Map<Class, Function<?, String>> defaultParamTypeEncoders) {
        this.client = client;
        this.metadataMap = metadataMap;
        this.defaultParamTypeEncoders = defaultParamTypeEncoders;
    }

    @Override
    public Object intercept(Object calledObject,
                            Method calledMethod,
                            Object[] execParams,
                            MethodProxy methodProxy) throws Throwable {
        if(isAnnotated(calledMethod)) {
            final MethodMetadata callMetadata = extractCallMetadata(calledMethod);
            return defaultIntercept(execParams, callMetadata);
        } else {
            return calledMethod.invoke(calledObject, execParams);
        }

    }

    Object defaultIntercept(Object[] execParams, MethodMetadata callMetadata) {
        final String urlWithParams = buildUrlWithParams(callMetadata, execParams);
        final WebClient.RequestBodySpec spec = initRequest(callMetadata, urlWithParams);
        addDefaultHeaders(spec, callMetadata);
        addHeadersParam(spec, callMetadata, execParams);
        addBodyParam(spec, callMetadata, execParams);
        return runRequest(callMetadata, spec);
    }

    private boolean isAnnotated(Method calledMethod) {
        return metadataMap.containsKey(calledMethod);
    }

    MethodMetadata extractCallMetadata(Method calledMethod) {
        MethodMetadata metadata = metadataMap.get(calledMethod);
        if (metadata == null) {
            throw new RuntimeException(
                    String.format("Not found metadata for method %s",
                            calledMethod.getName()));
        }
        return metadata;
    }

    String buildUrlWithParams(MethodMetadata metadata,
                                      Object[] objects) {
        final String urlWithSlash = metadata.getUrl().startsWith("/")
                ? metadata.getUrl()
                : metadata.getUrl().substring(1, metadata.getUrl().length() - 1);
        final StringBuilder urlBuilder = new StringBuilder(urlWithSlash);
        updateUrlWithQueryParams(urlBuilder, metadata, objects);

        return updateUrlWithPathParams(urlBuilder, metadata, objects);
    }

    private String updateUrlWithPathParams(StringBuilder urlBuilder,
                                           MethodMetadata callMetadata,
                                           Object[] objects) {
        String url = urlBuilder.toString();
        final List<ParameterMetadata> parametersMetadata =
                callMetadata.getParametersMetadata();
        for (final ParameterMetadata paramMetadata : parametersMetadata) {
            if (isPathParam(paramMetadata)) {
                final NamedParameterMetadata namedMetadata = (NamedParameterMetadata)
                        paramMetadata;
                final String paramName = namedMetadata.getName();
                final int index = namedMetadata.getIndex();
                url = url.replace(
                        "{" + paramName + "}",
                        encodeParameter(objects[index]));
            }
        }
        return url;
    }

    private void updateUrlWithQueryParams(StringBuilder urlBuilder,
                                          MethodMetadata callMetadata,
                                          Object[] objects) {
        final List<ParameterMetadata> parametersMetadata =
                callMetadata.getParametersMetadata();
        boolean added = false;
        for (int i = 0; i < parametersMetadata.size(); i++) {
            final ParameterMetadata paramMetadata = parametersMetadata.get(i);
            if (isQueryParam(paramMetadata)) {
                final NamedParameterMetadata namedMetadata = (NamedParameterMetadata)
                        paramMetadata;
                if (!added) {
                    urlBuilder.append("?");
                    added = true;
                }
                urlBuilder.append(namedMetadata.getName())
                        .append("=")
                        .append(encodeParameter(objects[i]));
            }
        }
    }

    void addBodyParam(WebClient.RequestBodySpec bodySpec,
                              MethodMetadata callMetadata,
                              Object[] parametersExecution) {
        final Function<Object, Object> encodeFunction =body -> encodeBody(
                body,
                callMetadata.getBodyEncoder());
        if (isMethodWithBody(callMetadata)) {
            callMetadata.getParametersMetadata()
                    .stream()
                    .filter(ReactiveHttpInterceptor::isBodyParam)
                    .mapToInt(ParameterMetadata::getIndex)
                    .mapToObj(index -> parametersExecution[index])
                    .map(encodeFunction)
                    .findFirst()
                    .map(BodyInserters::fromObject)
                    .ifPresent(bodySpec::body);
        }
    }

    void addDefaultHeaders(WebClient.RequestBodySpec spec,
                                   MethodMetadata callMetadata) {
        callMetadata.getDefaultHeaders()
                .forEach((key, value) -> spec.header(key, value.toArray(new String[0])));
    }

    void addHeadersParam(WebClient.RequestBodySpec bodySpec,
                                 MethodMetadata callMetadata,
                                 Object[] paramsExecution) {
        callMetadata.getParametersMetadata().stream()
                .filter(ReactiveHttpInterceptor::isHeaderParam)
                .map(param -> (NamedParameterMetadata) param)
                .forEach(param ->
                        bodySpec.header(
                                param.getName(),
                                encodeParameter(paramsExecution[param.getIndex()])));
    }

    Mono<Object> runRequest(MethodMetadata metadata,
                            WebClient.RequestBodySpec bodySpec) {
        try {
            return bodySpec
                    .exchange()
                    .flatMap(res -> res.bodyToMono(metadata.getParameterizedType()));
        } catch (Exception ex) {
            throw new ReactiveHttpRuntimeException(ex);
        }
    }

    WebClient.RequestBodySpec initRequest(MethodMetadata metadata,
                                                  String processedUrl) {
        return client.method(metadata.getHttpMethod())
                .uri(processedUrl)
                .contentType(metadata.getContentType());
    }

    private boolean isMethodWithBody(MethodMetadata callMetadata) {
        final HttpMethod callMethod = callMetadata.getHttpMethod();
        return POST.equals(callMethod)
                | PUT.equals(callMethod)
                || PATCH.equals(callMethod);
    }

    private static boolean isPathParam(ParameterMetadata parameterMetadata) {
        return parameterMetadata instanceof PathParameterMetadata;
    }

    private static boolean isQueryParam(ParameterMetadata parameterMetadata) {
        return parameterMetadata instanceof QueryParameterMetadata;
    }

    private static boolean isHeaderParam(ParameterMetadata parameterMetadata) {
        return parameterMetadata instanceof HeaderParameterMetadata;
    }

    private static boolean isBodyParam(ParameterMetadata parameterMetadata) {
        return parameterMetadata instanceof BodyParameterMetadata;
    }

    @SuppressWarnings("unchecked")
    private String encodeParameter(Object param) {
        final Class c = param.getClass();
        final Function encoderFunction = defaultParamTypeEncoders.get(c);
        return encoderFunction != null ? (String) encoderFunction.apply(param) : param
                .toString();
    }

    @SuppressWarnings("unchecked")
    private Object encodeBody(Object body, Optional<BodyMapperObject> encoderOpt) {
        return encoderOpt.orElseGet(defaultBodyEncoder).encode(body);
    }
}
