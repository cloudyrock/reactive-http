package com.github.cloudyrock.reactivehttp;

public final class ReactiveHttpBuilder {

    private ReactiveHttpBuilder() {
    }

    public static ReactiveHttpBuilderJackson jacksonBuilder() {
        return ReactiveHttpBuilderImpl.getInstance(ReactiveHttpBuilderImpl.ParserType.JACKSON);
    }

    public static ReactiveHttpBuilderBase defaultBuilder() {
        return ReactiveHttpBuilderImpl.getDefaultInstance();
    }

}
