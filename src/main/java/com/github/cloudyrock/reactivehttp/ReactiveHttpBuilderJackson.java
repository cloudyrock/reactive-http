package com.github.cloudyrock.reactivehttp;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;

public interface ReactiveHttpBuilderJackson extends ReactiveHttpBuilderBase {

    ReactiveHttpBuilderJackson setEncoderMapper(ObjectMapper mapper);

    ReactiveHttpBuilderJackson setDecoderMapper(ObjectMapper mapper);
}
