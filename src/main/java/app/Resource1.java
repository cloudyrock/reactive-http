package app;

import org.github.cloudyrock.reactivehttp.annotations.BodyMapper;
import org.github.cloudyrock.reactivehttp.annotations.BodyParam;
import org.github.cloudyrock.reactivehttp.annotations.Header;
import org.github.cloudyrock.reactivehttp.annotations.QueryParam;
import org.github.cloudyrock.reactivehttp.annotations.ReactiveHttp;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpMethod.POST;

public interface Resource1 {

    @BodyMapper(BodyEncoderImpl.class)
    @Header(name = "my-header", value = "myHeader-value-method")
    @ReactiveHttp(url = "/single-player-game/whatever", httpMethod = POST)
    Mono<String> method(@QueryParam("queryParam") String variable, @BodyParam BodyClass body);


}
