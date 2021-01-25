package com.salaboy.conferences.c4p.rest;

import io.opentracing.Tracer;
import io.opentracing.contrib.spring.web.client.TracingExchangeFilterFunction;
import io.opentracing.contrib.spring.web.client.WebClientSpanDecorator;
import io.opentracing.contrib.spring.web.webfilter.TracingWebFilter;
import io.opentracing.contrib.spring.web.webfilter.WebFluxSpanDecorator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static com.salaboy.conferences.c4p.rest.C4PController.logRequest;

@SpringBootApplication
public class C4PApplication {

    public static void main(String[] args) {
        SpringApplication.run(C4PApplication.class, args);
    }

    @Bean
    public WebClient getWebClient(Tracer tracer) {
        return WebClient.builder()
                .filters(
                        (List<ExchangeFilterFunction> x) -> new ArrayList<ExchangeFilterFunction>() {{
                            add(new TracingExchangeFilterFunction(tracer, Collections.singletonList(new WebClientSpanDecorator.StandardTags())));
                            add(logRequest());
                        }})
                            .build();

    }


    @Configuration
    class TracingConfiguration {
        @Bean
        public TracingWebFilter tracingWebFilter(Tracer tracer) {
            return new TracingWebFilter(
                    tracer,
                    Integer.MIN_VALUE,               // Order
                    Pattern.compile(""),             // Skip pattern
                    Collections.emptyList(),         // URL patterns, empty list means all
                    Arrays.asList(new WebFluxSpanDecorator.StandardTags(), new WebFluxSpanDecorator.WebFluxTags())
            );
        }
    }
}
