package org.github.cloudyrock.reactivehttp;

@FunctionalInterface
public interface BodyMapper<T, R> {

    R encode(T body);

}
