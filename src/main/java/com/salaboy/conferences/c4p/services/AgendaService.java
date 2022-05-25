package com.salaboy.conferences.c4p.services;

import com.salaboy.conferences.c4p.model.AgendaItem;
import com.salaboy.conferences.c4p.model.Proposal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Random;



@Service
@Slf4j
public class AgendaService {
    @Value("${AGENDA_SERVICE:http://fmtok8s-agenda}")
    private String AGENDA_SERVICE;

    @Autowired
    private WebClient webClient;

    public void createAgendaItem(String bearer, Proposal proposal) {
        System.out.println("Bearer here: " + bearer);
        String[] days = {"Monday", "Tuesday"};
        String[] times = {"9:00 am", "10:00 am", "11:00 am", "1:00 pm", "2:00 pm", "3:00 pm", "4:00 pm", "5:00 pm"};
        Random random = new Random();
        int day = random.nextInt(2);
        int time = random.nextInt(8);
        // Try sending the request, if it fails, log
        AgendaItem agendaItem = new AgendaItem(proposal, proposal.getTitle(), proposal.getAuthor(), days[day], times[time]);

        WebClient.ResponseSpec responseSpec = webClient
                .post()
                .uri(AGENDA_SERVICE)
                .header("Authorization", bearer)
                .body(BodyInserters.fromValue(agendaItem))
                .retrieve();


        responseSpec.bodyToMono(String.class)
                .doOnError(t -> {
                    t.printStackTrace();
                    log.error(">> Error contacting Agenda Service (" + AGENDA_SERVICE + ") for Proposal: " + proposal.getId());
                })
                .doOnSuccess(s -> log.info("> Request Sent to Agenda Service (" + AGENDA_SERVICE + ") to add accepted Proposal from: " + proposal.getEmail()))
                .subscribe();

    }

}
