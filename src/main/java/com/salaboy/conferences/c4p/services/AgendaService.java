package com.salaboy.conferences.c4p.services;

import com.salaboy.conferences.c4p.config.ServiceConfiguration;
import com.salaboy.conferences.c4p.model.AgendaItem;
import com.salaboy.conferences.c4p.model.Proposal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Random;


@Component
public class AgendaService {

    private static final Logger log = LoggerFactory.getLogger(AgendaService.class);

    @Autowired
    private WebClient.Builder webClient;

    @Autowired
    private ServiceConfiguration config;

    public Mono<AgendaItem> publishAgendaItem(String bearer, Proposal proposal) {
        log.info("Bearer Token: {} ", bearer);
        String[] days = {"Monday", "Tuesday"};
        String[] times = {"9:00 am", "10:00 am", "11:00 am", "1:00 pm", "2:00 pm", "3:00 pm", "4:00 pm", "5:00 pm"};
        Random random = new Random();
        int day = random.nextInt(2);
        int time = random.nextInt(8);

        // Try sending the request, if it fails, log
        AgendaItem agendaItem = new AgendaItem(proposal, proposal.title(), proposal.author(), days[day], times[time]);
        log.info("Agenda Service URL {}", config.getAgenda());
        return webClient.build().post()
                .uri(config.getAgenda())
                .header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(agendaItem))
                .retrieve()
                .bodyToMono(AgendaItem.class)
                .doOnSuccess(result -> {
                    log.info("Agenda Item Published Title: {}, author: {}, day: {}, time: {}.", result.title(), result.author(), result.day(), result.time());
                })
                .doOnError(result -> {
                    result.fillInStackTrace();
                    log.error("Error Publishing Agenda Item. Cause: {}. Message: {}", result.getCause(), result.getMessage());
                });

    }

}
