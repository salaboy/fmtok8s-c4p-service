package com.salaboy.conferences.c4p.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class C4PMetrics {

    private final MeterRegistry meterRegistry;
    private Counter submissions;
    private Counter approved;
    private Counter rejected;

    public C4PMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        submissions = Counter.builder("c4p")
                .tag("type", "submissions")
                .description("The number of proposals received")
                .register(meterRegistry);

        approved = Counter.builder("c4p")
                .tag("type", "approved")
                .description("The number of proposals approved")
                .register(meterRegistry);

        rejected = Counter.builder("c4p")
                .tag("type", "rejected")
                .description("The number of proposals rejected")
                .register(meterRegistry);

    }

    public Counter getSubmissions() {
        return submissions;
    }

    public Counter getApproved() {
        return approved;
    }

    public Counter getRejected() {
        return rejected;
    }
}
