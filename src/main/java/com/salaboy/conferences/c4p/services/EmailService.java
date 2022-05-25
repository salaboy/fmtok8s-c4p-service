package com.salaboy.conferences.c4p.services;

import com.salaboy.conferences.c4p.model.Proposal;
import com.salaboy.conferences.c4p.model.ProposalDecision;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;


@Service
@Slf4j
public class EmailService {
    @Value("${EMAIL_SERVICE:http://fmtok8s-email}")
    private String EMAIL_SERVICE;

    @Autowired
    private WebClient webClient;

    public void notifySpeakerByEmail(String bearer,  ProposalDecision decision, Proposal proposal) {
        System.out.println("Bearer here: " + bearer);

        WebClient.ResponseSpec responseSpec = webClient
                .post()
                .uri(EMAIL_SERVICE + "/notification")
                .header("Authorization", bearer)
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
