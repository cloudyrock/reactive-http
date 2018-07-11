package org.github.cloudyrock.reactivehttp;

abstract class NamedParameterMetadata extends ParameterMetadata {


    private final String name;

    NamedParameterMetadata(int index, String name) {
        super(index);
        this.name = name;
    }

    String getName() {
        return name;
    }

}
