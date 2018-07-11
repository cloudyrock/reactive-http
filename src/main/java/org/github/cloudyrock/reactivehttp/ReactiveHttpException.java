package org.github.cloudyrock.reactivehttp;

//TODO define structure to provide feddback
public class ReactiveHttpException extends RuntimeException {

    ReactiveHttpException(Exception ex) {
        super(ex);
    }

}
