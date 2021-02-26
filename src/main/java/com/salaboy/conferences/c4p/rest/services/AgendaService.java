package com.salaboy.conferences.c4p.rest.services;

import com.salaboy.conferences.c4p.rest.model.AgendaItem;
import com.salaboy.conferences.c4p.rest.model.Proposal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Random;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

@Service
@Slf4j
public class AgendaService {
    @Value("${AGENDA_SERVICE:http://fmtok8s-agenda}")
    private String AGENDA_SERVICE;

    @Autowired
    private WebClient webClient;

    public void createAgendaItem( Proposal proposal) {
        String[] days = {"Monday", "Tuesday"};
        String[] times = {"9:00 am", "10:00 am", "11:00 am", "1:00 pm", "2:00 pm", "3:00 pm", "4:00 pm", "5:00 pm"};
        Random random = new Random();
        int day = random.nextInt(2);
        int time = random.nextInt(8);
        // Try sending the request, if it fails, log
        AgendaItem agendaItem = new AgendaItem(proposal.getId(), proposal.getTitle(), proposal.getAuthor(), days[day], times[time]);

        WebClient.RequestBodySpec uri = webClient
                .post()
                .uri(AGENDA_SERVICE);
//        if(authorizedClient != null){
//            uri.attributes(oauth2AuthorizedClient(authorizedClient));
//        }
        WebClient.ResponseSpec responseSpec = uri

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
