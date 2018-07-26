package com.github.cloudyrock.reactivehttp;

import com.github.cloudyrock.reactivehttp.annotations.BodyMapper;
import com.github.cloudyrock.reactivehttp.annotations.BodyParam;
import com.github.cloudyrock.reactivehttp.annotations.HeaderParam;
import com.github.cloudyrock.reactivehttp.annotations.PathParam;
import com.github.cloudyrock.reactivehttp.annotations.QueryParam;
import com.github.cloudyrock.reactivehttp.annotations.ReactiveHttp;
import com.github.cloudyrock.reactivehttp.annotations.Header;
import com.github.cloudyrock.reactivehttp.exception.ReactiveHttpConfigurationException;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

public final class ReactiveHttpBuilder {

    private final Map<Class, Function<?, String>> defaultParamEncoders = new HashMap<>();

    public <T extends Object> ReactiveHttpBuilder defaultParamEncoder(
            Class<T> clazz,
            Function<T, String> encoder) {
        defaultParamEncoders.put(clazz, encoder);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T target(Class<T> tClass, String url) {

        final Collector<Method, ?, Map<Method, MethodMetadata>> collectorFunction =
                Collectors.toMap(m -> m, ReactiveHttpBuilder::buildMethodMetadata);

        final Map<Method, MethodMetadata> methodMetadataMap = Stream
                .of(tClass.getMethods())
                .filter(ReactiveHttpBuilder::isAnnotated)
                .collect(collectorFunction);

        final Map<String, Set<String>> defaultHeaders = Stream
                .of(tClass.getAnnotationsByType(Header.class))
                .collect(groupingBy(Header::name, mapping(Header::value, toSet())));

        final ReactiveHttpInterceptor interceptor = new ReactiveHttpInterceptor(
                buildClient(url, defaultHeaders),
                methodMetadataMap,
                defaultParamEncoders);

        return (T) Enhancer.create(tClass, interceptor);

    }

    private static WebClient buildClient(String baseUrl,
                                         Map<String, Set<String>> headers) {
        final WebClient.Builder builder = WebClient
                .builder()
                .baseUrl(baseUrl);

        headers.keySet().forEach(key -> builder.defaultHeader(
                key, headers.get(key).toArray(new String[0])));

        return builder.build();
    }

    private static boolean isAnnotated(Method method) {
        return method.isAnnotationPresent(ReactiveHttp.class);
    }

    private static MethodMetadata buildMethodMetadata(Method method) {
        final ReactiveHttp annotation = method.getAnnotation(ReactiveHttp.class);

        return new MethodMetadata(
                annotation.httpMethod(),
                annotation.url(),
                MediaType.parseMediaType(annotation.contentType()),
                extractParameterizedType(method),
                buildParametersMetadata(method),
                extractDefaultHeadersMap(method),
                extractBodyEncoder(method));
    }

    private static BodyMapperObject extractBodyEncoder(Method method) {
        try{
            final BodyMapper ann = method.getAnnotation(BodyMapper.class);
            return ann != null ? ann.value().getConstructor().newInstance() : null;
        } catch (Exception ex) {
            throw new ReactiveHttpConfigurationException(ex);
        }

    }

    private static Map<String, Set<String>> extractDefaultHeadersMap(Method method) {
        return Stream.of(method.getAnnotationsByType(Header.class))
                .collect(groupingBy(Header::name, mapping(Header::value, toSet())));
    }

    private static Class extractParameterizedType(Method method) {
        final ParameterizedType parameterizedType =
                (ParameterizedType) method.getGenericReturnType();
        return (Class) parameterizedType.getActualTypeArguments()[0];
    }

    private static List<ParameterMetadata> buildParametersMetadata(Method method) {
        final List<ParameterMetadata> parameterMetadata = new ArrayList<>();
        for (int index = 0; index < method.getParameters().length; index++) {
            final Parameter parameter = method.getParameters()[index];
            parameterMetadata.add(buildParameter(index, parameter));
        }
        return parameterMetadata;
    }

    private static ParameterMetadata buildParameter(int index, Parameter parameter) {
        if (parameter.isAnnotationPresent(PathParam.class)) {
            return buildPathParameter(index, parameter);
        } else if (parameter.isAnnotationPresent(QueryParam.class)) {
            return buildQueryParameter(index, parameter);
        } else if (parameter.isAnnotationPresent(HeaderParam.class)) {
            return buildHeaderParameter(index, parameter);
        } else if (parameter.isAnnotationPresent(BodyParam.class)) {
            return buildBodyParam(index);
        } else {
            throw new RuntimeException(String.format(
                    "Parameter %s not annotated", parameter.getName()));
        }
    }

    private static ParameterMetadata buildPathParameter(int index, Parameter parameter) {
        final PathParam annotation = parameter.getAnnotation(PathParam.class);
        return new PathParameterMetadata(index, annotation.value());
    }

    private static ParameterMetadata buildQueryParameter(int index, Parameter parameter) {
        final QueryParam annotation = parameter.getAnnotation(QueryParam.class);
        return new QueryParameterMetadata(index, annotation.value());
    }

    private static ParameterMetadata buildHeaderParameter(int index, Parameter parameter) {
        final HeaderParam annotation = parameter.getAnnotation(HeaderParam.class);
        return new HeaderParameterMetadata(index, annotation.value());
    }

    private static ParameterMetadata buildBodyParam(int index) {
        return new BodyParameterMetadata(index);
    }

}
