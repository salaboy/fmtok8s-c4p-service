package com.salaboy.conferences.c4p.rest.services;

import com.salaboy.conferences.c4p.rest.model.Proposal;
import com.salaboy.conferences.c4p.rest.model.ProposalDecision;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import static com.salaboy.conferences.c4p.rest.C4PController.logRequest;

@Service
@Slf4j
public class EmailService {
    @Value("${EMAIL_SERVICE:http://fmtok8s-email}")
    private String EMAIL_SERVICE;

    @Autowired
    private WebClient webClient;

    public void notifySpeakerByEmail(ProposalDecision decision, Proposal proposal) {

        WebClient.ResponseSpec responseSpec = webClient
                .post()
                .uri(EMAIL_SERVICE + "/notification")
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
