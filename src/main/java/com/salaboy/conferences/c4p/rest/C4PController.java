package com.salaboy.conferences.c4p.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salaboy.conferences.c4p.rest.metrics.C4PMetrics;
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
import io.cloudevents.spring.webflux.CloudEventHttpMessageReader;
import io.cloudevents.spring.webflux.CloudEventHttpMessageWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
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

    @Autowired
    private C4PMetrics c4PMetrics;


    @Autowired
    private WebClient.Builder rest;

    @Configuration
    public static class CloudEventHandlerConfiguration implements CodecCustomizer {

        @Override
        public void customize(CodecConfigurer configurer) {
            configurer.customCodecs().register(new CloudEventHttpMessageReader());
            configurer.customCodecs().register(new CloudEventHttpMessageWriter());
        }

    }



    public C4PController(ProposalRepository proposalRepository) {

        this.proposalRepository = proposalRepository;
    }

    @PostMapping
    public ResponseEntity<Proposal> newProposal(@RequestBody Proposal proposal) {
        log.info("> REST ENDPOINT INVOKED for Accepting a new Proposal");
        var saved = proposalRepository.save(proposal);
        c4PMetrics.getSubmissions().increment();
        log.info("> \t EventsEnabled: " + eventsEnabled);
        if (eventsEnabled) {
            try {
                emitNewProposalEvent(proposal);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        return ResponseEntity.ok().body(saved);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity deleteProposal(@PathVariable("id") String id) {
        log.info("> REST ENDPOINT INVOKED for Deleting a Proposal by Id: " + id);
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
        log.info("> REST ENDPOINT INVOKED for Deleting all Proposals");
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
        log.info("> REST ENDPOINT INVOKED for Getting a Proposal by Id: " + id);
        return proposalRepository.findById(id);
    }

    @PostMapping(value = "/{id}/decision")
    public void decide(@PathVariable("id") String id, @RequestBody ProposalDecision decision, @RequestHeader Map<String, String> headers) {
        log.info("> REST ENDPOINT INVOKED for Making a Decision for a Proposal");
        log.info("> Proposal Approved ( " + ((decision.isApproved()) ? "Approved" : "Rejected") + ")");
        log.info("> Headers: \n");
        for(String key : headers.keySet()){
            log.info("> Header Key: " + key + " - > Value: " + headers.get(key));
        }

        var proposalOptional = proposalRepository.findById(id);

        if (proposalOptional.isPresent()) {
            var proposal = proposalOptional.get();
            if (decision.isApproved()) {
                proposal.approve();
            } else {
                proposal.reject();
            }
            proposalRepository.save(proposal);
            log.info("> \t EventsEnabled: " + eventsEnabled);
            if (eventsEnabled) {
                try {
                    emitProposalDecisionMadeEvent(proposal);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }

            if (decision.isApproved()) {
                agendaService.createAgendaItem(headers.get("Authorization"), proposal);
                c4PMetrics.getApproved().increment();
            }else{
                c4PMetrics.getRejected().increment();
            }

            // Notify Potential Speaker By Email
            emailService.notifySpeakerByEmail(headers.get("Authorization"), decision, proposal);


        } else {

            log.error(">> Proposal Not Found (" + id + ")");
        }
    }


    private void emitNewProposalEvent(Proposal proposal) throws JsonProcessingException {
        CloudEventBuilder cloudEventBuilder = CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withType("C4P.ProposalReceived")
                .withSource(URI.create("c4p-service.default.svc.cluster.local"))
                .withData(objectMapper.writeValueAsString(proposal).getBytes(StandardCharsets.UTF_8))
                .withDataContentType("application/json; charset=UTF-8")
                .withSubject(proposal.getTitle());

        CloudEvent cloudEvent = cloudEventBuilder.build();

        logCloudEvent(cloudEvent);

        log.info("Producing CloudEvent with Proposal: " + proposal);

        rest.baseUrl(K_SINK).filter(logRequest()).build()
                .post().bodyValue(cloudEvent)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(t -> t.printStackTrace())
                .doOnSuccess(s -> log.info("Result -> " + s)).subscribe();

    }


    private void emitProposalDecisionMadeEvent(Proposal proposal) throws JsonProcessingException {


        CloudEventBuilder cloudEventBuilder = CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withType("C4P.ProposalDecisionMade")
                .withSource(URI.create("c4p-service.default.svc.cluster.local"))
                .withData(objectMapper.writeValueAsString(proposal).getBytes(StandardCharsets.UTF_8))
                .withDataContentType("application/json; charset=UTF-8")
                .withSubject(proposal.getTitle());

        CloudEvent cloudEvent = cloudEventBuilder.build();

        logCloudEvent(cloudEvent);

        log.info("Producing CloudEvent with Proposal: " + proposal);

        rest.baseUrl(K_SINK).filter(logRequest()).build()
                .post().bodyValue(cloudEvent)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(t -> t.printStackTrace())
                .doOnSuccess(s -> log.info("Result -> " + s)).subscribe();

    }


    private void logCloudEvent(CloudEvent cloudEvent) {
        EventFormat format = EventFormatProvider
                .getInstance()
                .resolveFormat(JsonFormat.CONTENT_TYPE);

        log.info("Cloud Event: " + new String(format.serialize(cloudEvent)));

    }

    public static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info("Request: " + clientRequest.method() + " - " + clientRequest.url());
            clientRequest.headers().forEach((name, values) -> values.forEach(value -> log.info(name + "=" + value)));
            return Mono.just(clientRequest);
        });
    }

}
