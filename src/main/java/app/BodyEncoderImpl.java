package app;

import org.github.cloudyrock.reactivehttp.BodyMapper;

public class BodyEncoderImpl implements BodyMapper<BodyClass, BodyClass> {
    @Override
    public BodyClass encode(BodyClass body) {
        return new BodyClass("this is the encoded value");
    }
}
