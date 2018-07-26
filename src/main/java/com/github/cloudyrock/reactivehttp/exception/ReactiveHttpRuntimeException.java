package com.github.cloudyrock.reactivehttp.exception;

//TODO define structure to provide feedback
public final class ReactiveHttpRuntimeException extends RuntimeException {

    public ReactiveHttpRuntimeException(Exception ex) {
        super(ex);
    }

}
