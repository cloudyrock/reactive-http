package app;

import org.github.cloudyrock.reactivehttp.ReactiveHttpBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.LocalDate;
import java.util.function.Function;

@SpringBootApplication
public class Application {

	public static void main(String[] args) throws NoSuchMethodException {
		SpringApplication.run(Application.class, args);

        final Resource1 r = injectResource();

        final String variable1 = "pathVariable";
        final BodyClass body = new BodyClass("my value");
        r.method(variable1, body).subscribe(System.out::println);
	}

    private static Resource1 injectResource() throws NoSuchMethodException {
        return new ReactiveHttpBuilder()
                    .defaultParamEncoder(LocalDate.class, LocalDate::toString)
                    .target(Resource1.class, "http://localhost:8081");
    }
}
