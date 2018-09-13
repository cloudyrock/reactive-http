package com.github.cloudyrock.reactivehttp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.cloudyrock.dimmer.DimmerFeature;
import com.github.cloudyrock.dimmer.FeatureExecutor;
import com.github.cloudyrock.reactivehttp.annotations.BodyParam;
import com.github.cloudyrock.reactivehttp.annotations.Header;
import com.github.cloudyrock.reactivehttp.annotations.HeaderParam;
import com.github.cloudyrock.reactivehttp.annotations.PathParam;
import com.github.cloudyrock.reactivehttp.annotations.QueryParam;
import com.github.cloudyrock.reactivehttp.annotations.ReactiveHttp;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;
import static org.springframework.http.MediaType.APPLICATION_JSON;

final class ReactiveHttpBuilderImpl implements ReactiveHttpBuilderJackson {

    private ObjectMapper jacksonDecoderMapper;
    private ObjectMapper jacksonEncoderMapper;

    private final ParserType parserType;
    private FeatureExecutor featureExecutor;

    enum ParserType {JACKSON}

    static ReactiveHttpBuilderImpl getInstance(ParserType parserType) {
        return new ReactiveHttpBuilderImpl(parserType);
    }

    static ReactiveHttpBuilderImpl getDefaultInstance() {
        return new ReactiveHttpBuilderImpl(ParserType.JACKSON);
    }

    private ReactiveHttpBuilderImpl(ParserType parserType) {
        this.parserType = parserType;
    }

    @Override
    public ReactiveHttpBuilderBase dimmerFeatureExecutor(FeatureExecutor featureExecutor) {
        this.featureExecutor = featureExecutor;
        return this;
    }

    @Override
    public ReactiveHttpBuilderJackson setEncoderMapper(ObjectMapper mapper) {
        jacksonEncoderMapper = mapper;
        return this;
    }

    @Override
    public ReactiveHttpBuilderJackson setDecoderMapper(ObjectMapper mapper) {
        jacksonDecoderMapper = mapper;
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T target(Class<T> tClass, String host) {

        final Collector<Method, ?, Map<Method, MethodMetadata>> collectorFunction =
                Collectors.toMap(m -> m, ReactiveHttpBuilderImpl::buildMethodMetadata);

        final Map<Method, MethodMetadata> methodMetadataMap = Stream
                .of(tClass.getMethods())
                .filter(ReactiveHttpBuilderImpl::isAnnotated)
                .collect(collectorFunction);

        final Map<String, Set<String>> defaultHeaders = Stream
                .of(tClass.getAnnotationsByType(Header.class))
                .collect(groupingBy(Header::name, mapping(Header::value, toSet())));

        final ReactiveHttpInterceptor interceptor;
        if (featureExecutor != null) {
            interceptor = new ReactiveHttpDimmerInterceptor(
                    buildClient(host, defaultHeaders),
                    methodMetadataMap,
                    featureExecutor);
        } else {
            interceptor = new ReactiveHttpInterceptor(
                    buildClient(host, defaultHeaders),
                    methodMetadataMap);
        }
        return (T) Enhancer.create(tClass, interceptor);

    }

    private WebClient buildClient(String baseUrl,
                                  Map<String, Set<String>> headers) {
        final ExchangeStrategies strategies = getExchangeStrategies();

        final WebClient.Builder builder = WebClient
                .builder()
                .exchangeStrategies(strategies)
                .baseUrl(baseUrl);

        headers.keySet().forEach(key -> builder.defaultHeader(key, headers.get(key).toArray(new String[0])));

        return builder.build();
    }

    private ExchangeStrategies getExchangeStrategies() {
        if(ParserType.JACKSON.equals(parserType)) {

            if(jacksonEncoderMapper == null ) {
                jacksonEncoderMapper = new ObjectMapper();
                jacksonEncoderMapper.registerModule(new Jdk8Module());
            }

            if(jacksonDecoderMapper == null ) {
                jacksonDecoderMapper = new ObjectMapper();
                jacksonDecoderMapper.registerModule(new Jdk8Module());
            }

            return ExchangeStrategies
                    .builder()
                    .codecs(codecConfigurer -> {
                        final ClientCodecConfigurer.ClientDefaultCodecs defaultCodecs = codecConfigurer.defaultCodecs();
                        defaultCodecs.jackson2JsonEncoder(new Jackson2JsonEncoder(jacksonEncoderMapper, APPLICATION_JSON));
                        defaultCodecs.jackson2JsonDecoder(new Jackson2JsonDecoder(jacksonDecoderMapper, APPLICATION_JSON));
                    }).build();
        } else {
            throw new RuntimeException("Unrecognized ParserType " + this.parserType);
        }

    }

    private static boolean isAnnotated(Method method) {
        return method.isAnnotationPresent(ReactiveHttp.class);
    }

    private static MethodMetadata buildMethodMetadata(Method method) {
        final ReactiveHttp annotation = method.getAnnotation(ReactiveHttp.class);
        final DimmerFeature dimmerFeature = method.getAnnotation(DimmerFeature.class);

        return new MethodMetadata(
                annotation.httpMethod(),
                annotation.url(),
                MediaType.parseMediaType(annotation.contentType()),
                extractParameterizedType(method),
                buildParametersMetadata(method),
                extractDefaultHeadersMap(method),
                dimmerFeature != null ? dimmerFeature.value() : null);
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
