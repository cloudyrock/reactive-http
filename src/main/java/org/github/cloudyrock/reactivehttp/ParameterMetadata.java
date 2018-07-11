package org.github.cloudyrock.reactivehttp;

abstract class ParameterMetadata {


    private final int index;

    ParameterMetadata(int index) {
        this.index = index;
    }

    int getIndex() {
        return index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ParameterMetadata that = (ParameterMetadata) o;

        return index == that.index;
    }

    @Override
    public int hashCode() {
        return index;
    }
}
