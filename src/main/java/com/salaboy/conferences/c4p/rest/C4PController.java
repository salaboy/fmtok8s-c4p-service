package com.salaboy.conferences.c4p.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salaboy.cloudevents.helper.CloudEventsHelper;
import com.salaboy.conferences.c4p.rest.model.Proposal;
import com.salaboy.conferences.c4p.rest.model.ProposalDecision;
import com.salaboy.conferences.c4p.rest.model.ProposalStatus;
import com.salaboy.conferences.c4p.rest.services.AgendaService;
import com.salaboy.conferences.c4p.rest.services.EmailService;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;
import io.zeebe.cloudevents.ZeebeCloudEventsHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.*;

@RestController
@Slf4j
public class C4PController {

    private final ProposalRepository proposalRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AgendaService agendaService;

    @Autowired
    private EmailService emailService;

    @Value("${EVENTS_ENABLED:false}")
    private Boolean eventsEnabled;

    @Value("${K_SINK:http://broker-ingress.knative-eventing.svc.cluster.local/default/default}")
    private String K_SINK;


    public C4PController(ProposalRepository proposalRepository) {

        this.proposalRepository = proposalRepository;
    }

    @PostMapping
    public ResponseEntity<Proposal> newProposal(@RequestBody Proposal proposal) {

        var saved = proposalRepository.save(proposal);

        emitNewProposalEvent(proposal);

        return ResponseEntity.ok().body(saved);
    }

    private void emitNewProposalEvent(Proposal proposal) {
        if(eventsEnabled) {
            String proposalString = null;
            try {
                proposalString = objectMapper.writeValueAsString(proposal);
                proposalString = objectMapper.writeValueAsString(proposalString); //needs double quoted ??
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            CloudEventBuilder cloudEventBuilder = CloudEventBuilder.v1()
                    .withId(UUID.randomUUID().toString())
                    .withTime(OffsetDateTime.now().toZonedDateTime()) // bug-> https://github.com/cloudevents/sdk-java/issues/200
                    .withType("C4P.ProposalReceived")
                    .withSource(URI.create("c4p-service.default.svc.cluster.local"))
                    .withData(proposalString.getBytes())
                    .withDataContentType("application/json")
                    .withSubject(proposal.getTitle());

            CloudEvent zeebeCloudEvent = ZeebeCloudEventsHelper
                    .buildZeebeCloudEvent(cloudEventBuilder)
                    .withCorrelationKey(proposal.getId()).build();

            logCloudEvent(zeebeCloudEvent);
            WebClient webClient = WebClient.builder().baseUrl(K_SINK).filter(logRequest()).build();

            WebClient.ResponseSpec postCloudEvent = CloudEventsHelper.createPostCloudEvent(webClient, zeebeCloudEvent);

            postCloudEvent.bodyToMono(String.class)
                    .doOnError(t -> t.printStackTrace())
                    .doOnSuccess(s -> log.info("Cloud Event Posted to K_SINK -> " + K_SINK + ": Result: " +  s))
                    .subscribe();
        }
    }

    private void emitProposalDecisionMadeEvent(Proposal proposal) {
        if(eventsEnabled) {
            String proposalString = null;
            try {
                proposalString = objectMapper.writeValueAsString(proposal);
                proposalString = objectMapper.writeValueAsString(proposalString); //needs double quoted ??
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            CloudEventBuilder cloudEventBuilder = CloudEventBuilder.v1()
                    .withId(UUID.randomUUID().toString())
                    .withTime(OffsetDateTime.now().toZonedDateTime()) // bug-> https://github.com/cloudevents/sdk-java/issues/200
                    .withType("C4P.ProposalDecisionMade")
                    .withSource(URI.create("c4p-service.default.svc.cluster.local"))
                    .withData(proposalString.getBytes())
                    .withDataContentType("application/json")
                    .withSubject(proposal.getTitle());

            CloudEvent zeebeCloudEvent = ZeebeCloudEventsHelper
                    .buildZeebeCloudEvent(cloudEventBuilder)
                    .withCorrelationKey(proposal.getId()).build();

            logCloudEvent(zeebeCloudEvent);
            WebClient webClient = WebClient.builder().baseUrl(K_SINK).filter(logRequest()).build();

            WebClient.ResponseSpec postCloudEvent = CloudEventsHelper.createPostCloudEvent(webClient, zeebeCloudEvent);

            postCloudEvent.bodyToMono(String.class)
                    .doOnError(t -> t.printStackTrace())
                    .doOnSuccess(s -> log.info("Cloud Event Posted to K_SINK -> " + K_SINK + ": Result: " +  s))
                    .subscribe();
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity deleteProposal(@PathVariable("id") String id) {

        var optionalProposal = proposalRepository.findById(id);

        if (optionalProposal.isPresent()) {

            proposalRepository.deleteById(id);

            return new ResponseEntity<>(HttpStatus.OK);

        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/")
    public ResponseEntity<Void> deleteProposals() {

        proposalRepository.deleteAll();

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping()
    public Set<Proposal> getAll(@RequestParam(value = "pending", defaultValue = "false", required = false) boolean pending) {

        if (!pending) {
            return new HashSet<>(proposalRepository.findAll());
        } else {
            return new HashSet<>(proposalRepository.findAllByStatus(ProposalStatus.PENDING));
        }
    }

    @GetMapping("/{id}")
    public Optional<Proposal> getById(@PathVariable("id") final String id) {

        return proposalRepository.findById(id);
    }

    @PostMapping(value = "/{id}/decision")
    public void decide(@PathVariable("id") String id, @RequestBody ProposalDecision decision) {

        log.info("> Proposal Approved Event ( " + ((decision.isApproved()) ? "Approved" : "Rejected") + ")");
        var proposalOptional = proposalRepository.findById(id);

        if (proposalOptional.isPresent()) {
            var proposal = proposalOptional.get();
            if(decision.isApproved()) {
                proposal.approve();
            }else{
                proposal.reject();
            }
            proposalRepository.save(proposal);

            if (decision.isApproved()) {
                agendaService.createAgendaItem(proposal);
                emitProposalDecisionMadeEvent(proposal);
            }

            // Notify Potential Speaker By Email
            emailService.notifySpeakerByEmail(decision, proposal);


        } else {

            log.error(" Proposal Not Found Event (" + id + ")");
        }
    }


    private void logCloudEvent(CloudEvent cloudEvent) {
        EventFormat format = EventFormatProvider
                .getInstance()
                .resolveFormat(JsonFormat.CONTENT_TYPE);

        log.info("Cloud Event: " + new String(format.serialize(cloudEvent)));

    }

    private static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info("Request: " + clientRequest.method() + " - " + clientRequest.url());
            clientRequest.headers().forEach((name, values) -> values.forEach(value -> log.info(name + "=" + value)));
            return Mono.just(clientRequest);
        });
    }
}
