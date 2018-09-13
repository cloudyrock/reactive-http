package com.github.cloudyrock.reactivehttp;

import com.github.cloudyrock.dimmer.FeatureExecutor;

public interface ReactiveHttpBuilderBase {

    ReactiveHttpBuilderBase dimmerFeatureExecutor(FeatureExecutor featureExecutor);

    @SuppressWarnings("unchecked")
    <T> T target(Class<T> tClass, String host);

}
