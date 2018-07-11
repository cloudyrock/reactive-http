package org.github.cloudyrock.reactivehttp;

import org.github.cloudyrock.reactivehttp.annotations.Header;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

//todo serialization of complex objects
class ReactiveInterceptor implements MethodInterceptor {

    private final WebClient client;
    private final Map<Method, MethodMetadata> metadataMap;
    private final Map<Class, Function<Object, String>> defaultParamTypeEncoders;
    private final Map<Class, Function<Object, Object>> defaultBodyTypeEncoders;

    ReactiveInterceptor(WebClient client,
                        Map<Method, MethodMetadata> metadataMap,
                        Map<Class, Function<Object, String>> defaultParamTypeEncoders,
                        Map<Class, Function<Object, Object>> defaultBodyTypeEncoders) {
        this.client = client;
        this.metadataMap = metadataMap;
        this.defaultParamTypeEncoders = defaultParamTypeEncoders;
        this.defaultBodyTypeEncoders = defaultBodyTypeEncoders;
    }

    @Override
    public Object intercept(Object calledObject,
                            Method calledMethod,
                            Object[] execParams,
                            MethodProxy methodProxy) throws Throwable {
        final MethodMetadata callMetadata = extractCallMetadata(calledMethod);
        final String urlWithParams = buildUrlWithParams(callMetadata, execParams);
        final WebClient.RequestBodySpec spec = initRequest(callMetadata, urlWithParams);
        addHeadersAnnotations(spec, calledMethod);
        addHeadersParam(spec, callMetadata, execParams);
        addBodyParam(spec, callMetadata, execParams);
        return runRequest(callMetadata, spec);
    }

    private MethodMetadata extractCallMetadata(Method calledMethod) {
        MethodMetadata metadata = metadataMap.get(calledMethod);
        if (metadata == null) {
            throw new RuntimeException(
                    String.format("Not found metadata for method %s",
                            calledMethod.getName()));
        }
        return metadata;
    }

    private String buildUrlWithParams(MethodMetadata metadata,
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

    private void addBodyParam(WebClient.RequestBodySpec bodySpec,
                              MethodMetadata callMetadata,
                              Object[] parametersExecution) {
        if (isMethodWithBody(callMetadata)) {
            callMetadata.getParametersMetadata()
                    .stream()
                    .filter(ReactiveInterceptor::isBodyParam)
                    .mapToInt(ParameterMetadata::getIndex)
                    .mapToObj(index -> parametersExecution[index])
                    .map(this::encodeBody)
                    .findFirst()
                    .map(BodyInserters::fromObject)
                    .ifPresent(bodySpec::body);
        }
    }

    private void addHeadersAnnotations(WebClient.RequestBodySpec spec, Method method) {
        Stream.of(method.getAnnotationsByType(Header.class))
                .collect(groupingBy(Header::name, mapping(Header::value, toSet())))
                .forEach((key, value) -> spec.header(key, value.toArray(new String[0])));
    }

    private void addHeadersParam(WebClient.RequestBodySpec bodySpec,
                                 MethodMetadata callMetadata,
                                 Object[] paramsExecution) {
        callMetadata.getParametersMetadata().stream()
                .filter(ReactiveInterceptor::isHeaderParam)
                .map(param -> (NamedParameterMetadata) param)
                .forEach(param ->
                        bodySpec.header(
                                param.getName(),
                                encodeParameter(paramsExecution[param.getIndex()])));
    }

    private Mono<Object> runRequest(MethodMetadata metadata,
                                    WebClient.RequestBodySpec bodySpec) {
        try {
            return bodySpec
                    .exchange()
                    .flatMap(res -> res.bodyToMono(metadata.getParameterizedType()));
        } catch (Exception ex) {
            throw new ReactiveHttpException(ex);
        }
    }

    //TODO inject encoders. Quick work-around providing functions as mappers
    //like LocalDateMapper = Function<LocalDate, String> m = d-> d.toString();
    //builder.defaultParamEncoder(m);
    private WebClient.RequestBodySpec initRequest(MethodMetadata metadata,
                                                  String processedUrl) {
        return client.method(metadata.getMethod())
                .uri(processedUrl)
                .contentType(metadata.getContentType());
    }

    private boolean isMethodWithBody(MethodMetadata callMetadata) {
        final HttpMethod callMethod = callMetadata.getMethod();
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

    private String encodeParameter(Object param) {
        final Class c = param.getClass();
        final Function<Object, String> encoderFunction = defaultParamTypeEncoders.get(c);
        return encoderFunction != null ? encoderFunction.apply(param) : param.toString();
    }

    private Object encodeBody(Object body) {
        final Class c = body.getClass();
        final Function<Object, Object> encoderFunction = defaultBodyTypeEncoders.get(c);
        return encoderFunction != null ? encoderFunction.apply(body) : body;
    }
}
