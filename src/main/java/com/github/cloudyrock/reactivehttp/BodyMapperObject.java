package com.github.cloudyrock.reactivehttp;

@FunctionalInterface
public interface BodyMapperObject<T, R> {

    R encode(T body);

}
