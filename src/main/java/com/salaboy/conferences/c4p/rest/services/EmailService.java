package com.salaboy.conferences.c4p.rest.services;

import com.salaboy.conferences.c4p.rest.model.Proposal;
import com.salaboy.conferences.c4p.rest.model.ProposalDecision;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

@Service
@Slf4j
public class EmailService {
    @Value("${EMAIL_SERVICE:http://fmtok8s-email}")
    private String EMAIL_SERVICE;

    @Autowired
    private WebClient webClient;

    public void notifySpeakerByEmail(  ProposalDecision decision, Proposal proposal) {


        WebClient.RequestBodySpec uri = webClient
                .post()
                .uri(EMAIL_SERVICE + "/notification");
//        if(authorizedClient != null){
//            uri.attributes(oauth2AuthorizedClient(authorizedClient));
//        }
        WebClient.ResponseSpec responseSpec = uri
                .body(BodyInserters.fromValue(proposal))
                .retrieve();
        responseSpec.bodyToMono(String.class)
                .doOnError(t -> {
                    t.printStackTrace();
                    log.error(">> Error contacting Email Service ("+EMAIL_SERVICE+") for Proposal: " + proposal.getId() );
                })
                .doOnSuccess(s -> log.info("> Request sent to Email Service ("+EMAIL_SERVICE+") about proposal from: " + proposal.getEmail() + " -> " + ((decision.isApproved()) ? "Approved" : "Rejected") + ")"))
                .subscribe();
    }
}
