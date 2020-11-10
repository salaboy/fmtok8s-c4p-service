package com.salaboy.conferences.c4p.rest.services;

import com.salaboy.conferences.c4p.rest.model.AgendaItem;
import com.salaboy.conferences.c4p.rest.model.Proposal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Random;

@Service
@Slf4j
public class AgendaService {
    @Value("${AGENDA_SERVICE:http://fmtok8s-agenda}")
    private String AGENDA_SERVICE;

    private RestTemplate restTemplate = new RestTemplate();

    public void createAgendaItem(Proposal proposal) {
        log.info("> Add Proposal To Agenda Event ");
        String[] days = {"Monday", "Tuesday"};
        String[] times = {"9:00 am", "10:00 am", "11:00 am", "1:00 pm", "2:00 pm", "3:00 pm", "4:00 pm", "5:00 pm"};
        Random random = new Random();
        int day = random.nextInt(2);
        int time = random.nextInt(8);
        // Try sending the request, if it fails, log
        try {
            HttpEntity<AgendaItem> requestAgenda = new HttpEntity<>(new AgendaItem(proposal.getId(), proposal.getTitle(), proposal.getAuthor(), days[day], times[time]));
            restTemplate.postForEntity(AGENDA_SERVICE, requestAgenda, String.class);
        } catch(Exception ex){
            log.error(">> Error contacting Agenda Service ("+AGENDA_SERVICE+") for Proposal: " + proposal.getId());
            ex.printStackTrace();
        }

    }

}
