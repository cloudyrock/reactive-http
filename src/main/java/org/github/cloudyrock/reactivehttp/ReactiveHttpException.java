package org.github.cloudyrock.reactivehttp;

//TODO define structure to provide feedback
public final class ReactiveHttpException extends RuntimeException {

    ReactiveHttpException(Exception ex) {
        super(ex);
    }

}
