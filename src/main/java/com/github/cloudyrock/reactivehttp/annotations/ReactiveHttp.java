package com.github.cloudyrock.reactivehttp.annotations;

import org.springframework.core.annotation.AliasFor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ReactiveHttp {

    @AliasFor("value")
    String url();

    HttpMethod httpMethod();

    String contentType() default MediaType.APPLICATION_JSON_VALUE;
}
