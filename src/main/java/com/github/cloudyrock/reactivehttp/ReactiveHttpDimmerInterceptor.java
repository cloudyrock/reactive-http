package com.github.cloudyrock.reactivehttp;

import com.github.cloudyrock.dimmer.FeatureExecutor;
import com.github.cloudyrock.dimmer.FeatureInvocation;
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

class ReactiveHttpDimmerInterceptor extends ReactiveHttpInterceptor {

    private final FeatureExecutor featureExecutor;

    ReactiveHttpDimmerInterceptor(WebClient client,
                                  Map<Method, MethodMetadata> metadataMap,
                                  Map<Class, Function<?, String>> defaultParamTypeEncoders,
                                  FeatureExecutor featureExecutor) {
        super(client, metadataMap, defaultParamTypeEncoders);
        this.featureExecutor = featureExecutor;
    }

    @Override
    public Object intercept(Object calledObject,
                            Method calledMethod,
                            Object[] execParams,
                            MethodProxy methodProxy) throws Throwable {

        final MethodMetadata callMetadata = extractCallMetadata(calledMethod);
        final String urlWithParams = buildUrlWithParams(callMetadata, execParams);
        final WebClient.RequestBodySpec spec = initRequest(callMetadata, urlWithParams);
        addDefaultHeaders(spec, callMetadata);
        addHeadersParam(spec, callMetadata, execParams);
        addBodyParam(spec, callMetadata, execParams);

        if(callMetadata.getDimmerFeature().isPresent()) {
            final String feature = callMetadata.getDimmerFeature().get();
            final FeatureInvocation featureInvocation = createFeatureInvocation(
                    feature, calledObject, calledMethod, execParams);
            return featureExecutor.executeDimmerFeature(
                    feature,
                    featureInvocation,
                    () -> runRequest(callMetadata, spec));
        } else {
            return runRequest(callMetadata, spec);
        }
    }

    private FeatureInvocation createFeatureInvocation(String feature,
                                                      Object calledObject,
                                                      Method calledMethod,
                                                      Object[] execParams) {
        return new FeatureInvocation(
                feature,
                calledMethod.getName(),
                calledObject.getClass(),
                execParams,
                calledMethod.getReturnType());
    }

}
