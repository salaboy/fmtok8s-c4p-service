package com.salaboy.conferences.c4p.services;

import com.salaboy.conferences.c4p.config.ServiceConfiguration;
import com.salaboy.conferences.c4p.model.Notification;
import com.salaboy.conferences.c4p.model.Proposal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


@Component
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private WebClient.Builder webClient;

    @Autowired
    private ServiceConfiguration config;

    public Mono<Notification> notifySpeakerByEmail(String bearer, Proposal proposal) {
        log.info("Bearer here: {} ", bearer);
        log.info("Email Service URL {}", config.getEmailService());
        return webClient.build().post()
                .uri(config.getEmailService() + "/notification")
                .header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(proposal))
                .retrieve()
                .bodyToMono(Notification.class)
                .doOnSuccess(result -> {
                    log.info("Notification Sent Id: {}, title: {}, to: {}.", result.id(), result.title(), result.to());
                })
                .doOnError(result -> {
                    result.fillInStackTrace();
                    log.error("Error publishing event. Cause: {}. Message: {}", result.getCause(), result.getMessage());
                });
    }
}
