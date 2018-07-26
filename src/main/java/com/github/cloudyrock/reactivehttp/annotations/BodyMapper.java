package com.github.cloudyrock.reactivehttp.annotations;

import com.github.cloudyrock.reactivehttp.BodyMapperObject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface BodyMapper {

    Class<? extends BodyMapperObject> value();

}
