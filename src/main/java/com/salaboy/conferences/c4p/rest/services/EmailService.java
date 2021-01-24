package com.salaboy.conferences.c4p.rest.services;

import com.salaboy.conferences.c4p.rest.model.Proposal;
import com.salaboy.conferences.c4p.rest.model.ProposalDecision;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class EmailService {
    @Value("${EMAIL_SERVICE:http://fmtok8s-email}")
    private String EMAIL_SERVICE;

    @Autowired
    private RestTemplate restTemplate;

    public void notifySpeakerByEmail(ProposalDecision decision, Proposal proposal) {

        try {
            HttpEntity<Proposal> requestEmail = new HttpEntity<>(proposal);
            restTemplate.postForEntity(EMAIL_SERVICE + "/notification", requestEmail, String.class);
            log.info("> Request sent to Email Service ("+EMAIL_SERVICE+") about proposal from: " + proposal.getEmail() + " -> " + ((decision.isApproved()) ? "Approved" : "Rejected") + ")");
        }catch(Exception ex){
            log.error(">> Error contacting Email Service ("+EMAIL_SERVICE+") for Proposal: " + proposal.getId() );
            ex.printStackTrace();
        }
    }
}
