package org.github.cloudyrock.reactivehttp.annotations;

import org.github.cloudyrock.reactivehttp.BodyMapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface BodyMapper {

    Class<? extends org.github.cloudyrock.reactivehttp.BodyMapper> value();

}
