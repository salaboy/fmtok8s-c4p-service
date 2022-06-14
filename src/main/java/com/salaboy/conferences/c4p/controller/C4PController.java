package com.salaboy.conferences.c4p.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salaboy.conferences.c4p.metrics.C4PMetrics;
import com.salaboy.conferences.c4p.model.Proposal;
import com.salaboy.conferences.c4p.model.ProposalDecision;
import com.salaboy.conferences.c4p.model.ProposalStatus;
import com.salaboy.conferences.c4p.repository.ProposalRepository;
import com.salaboy.conferences.c4p.services.AgendaService;
import com.salaboy.conferences.c4p.services.EmailService;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;
import io.cloudevents.spring.webflux.CloudEventHttpMessageReader;
import io.cloudevents.spring.webflux.CloudEventHttpMessageWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping
public class C4PController {

    private static final Logger log = LoggerFactory.getLogger(C4PController.class);

    @Value("${EVENTS_ENABLED:false}")
    private Boolean eventsEnabled;

    @Value("${K_SINK:http://broker-ingress.knative-eventing.svc.cluster.local/default/default}")
    private String K_SINK;

    private final AgendaService agendaService;
    private final EmailService emailService;
    private final ProposalRepository proposalRepository;
    private final C4PMetrics c4PMetrics;
    private final WebClient.Builder webClient;

    private ObjectMapper objectMapper = new ObjectMapper();

    public C4PController(final AgendaService agendaService, final EmailService emailService,
                         final ProposalRepository proposalRepository, final C4PMetrics c4PMetrics,
                         final WebClient.Builder webClient) {
        this.agendaService = agendaService;
        this.emailService = emailService;
        this.proposalRepository = proposalRepository;
        this.c4PMetrics = c4PMetrics;
        this.webClient = webClient;
    }


    @Configuration
    public static class CloudEventHandlerConfiguration implements CodecCustomizer {

        @Override
        public void customize(CodecConfigurer configurer) {
            configurer.customCodecs().register(new CloudEventHttpMessageReader());
            configurer.customCodecs().register(new CloudEventHttpMessageWriter());
        }

    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Proposal> newProposal(@RequestBody Proposal proposal) {
        log.info("> REST ENDPOINT INVOKED for Creating a new Proposal: " + proposal);
        return proposalRepository.save(proposal).doOnSuccess(ai -> emitCloudEventForProposalAdded(ai));

    }

//    @PostMapping
//    public ResponseEntity<Proposal> newProposal(@RequestBody Proposal proposal) {
//        log.info("> REST ENDPOINT INVOKED for Accepting a new Proposal");
//        var saved = proposalRepository.save(proposal);
//        c4PMetrics.getSubmissions().increment();
//        log.info("> \t EventsEnabled: " + eventsEnabled);
//        if (eventsEnabled) {
//            try {
//                emitNewProposalEvent(proposal);
//            } catch (JsonProcessingException e) {
//                e.printStackTrace();
//            }
//        }
//
//        return ResponseEntity.ok().body(saved);
//    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Proposal>> deleteProposal(@PathVariable("id") Long id) {
        log.info("> REST ENDPOINT INVOKED for Deleting a Proposal by Id: " + id);
        return Mono.just(id)
                .flatMap(proposalRepository::findById)
                .flatMap(p -> proposalRepository.deleteById(p.id())
                        .thenReturn(p))
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));

    }

    @DeleteMapping("/")
    @ResponseStatus(HttpStatus.OK)
    public Mono<Void> deleteProposals() {
        log.info("> REST ENDPOINT INVOKED for Deleting all Proposals");
        return proposalRepository.deleteAll();
    }

    //
    @GetMapping()
    public Flux<Proposal> getAll(@RequestParam(value = "pending", defaultValue = "false", required = false) boolean pending) {
        return !pending ? proposalRepository.findAll() : proposalRepository.findAllByStatus(ProposalStatus.PENDING);
    }

    //
    @GetMapping("/{id}")
    public Mono<Proposal> getById(@PathVariable("id") final Long id) {
        log.info("> REST ENDPOINT INVOKED for Getting a Proposal by Id: " + id);
        return proposalRepository.findById(id);
    }

    @PostMapping(value = "/{id}/decision")
    public Mono<ResponseEntity<Proposal>> decide(@PathVariable("id") Long id, @RequestBody ProposalDecision decision, @RequestHeader Map<String, String> headers) {
        log.info("> REST ENDPOINT INVOKED for Making a Decision for a Proposal");
        log.info("> Proposal Approved ( " + ((decision.approved()) ? "Approved" : "Rejected") + ")");
        return Mono.just(id)
                .flatMap(proposalRepository::findById)
                .flatMap(proposal -> {
                    log.info("Proposal found: " + proposal + "with decision: " + decision);
                    if (decision.approved()) {
                        proposal = proposal.approve();
                    } else {
                        proposal = proposal.reject();
                    }
                    log.info("Saving Proposal: " + proposal);
                    return proposalRepository.save(proposal)
                            .map(savedProposal ->
                            {
                                emitProposalDecisionMadeEvent(savedProposal);
                                if (decision.approved()) {
                                    agendaService.publishAgendaItem(headers.get("Authorization"), savedProposal).subscribe();
                                    c4PMetrics.getApproved().increment();
                                } else {
                                    c4PMetrics.getRejected().increment();
                                }
                                // Notify Potential Speaker By Email
                                emailService.notifySpeakerByEmail(headers.get("Authorization"), savedProposal).subscribe();
                                return savedProposal;
                            });
                })
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));


    }

    //
//    @PostMapping(value = "/{id}/decision")
//    public void decide(@PathVariable("id") String id, @RequestBody ProposalDecision decision, @RequestHeader Map<String, String> headers) {
//        log.info("> REST ENDPOINT INVOKED for Making a Decision for a Proposal");
//        log.info("> Proposal Approved ( " + ((decision.approved()) ? "Approved" : "Rejected") + ")");
//        log.info("> Headers: \n");
//        for(String key : headers.keySet()){
//            log.info("> Header Key: " + key + " - > Value: " + headers.get(key));
//        }
//
//        var proposalOptional = proposalRepository.findById(id);
//
//        if (proposalOptional.isPresent()) {
//            var proposal = proposalOptional.get();
//            if (decision.approved()) {
//                proposal = proposal.approve();
//            } else {
//                proposal = proposal.reject();
//            }
//            proposalRepository.save(proposal);
//            log.info("> \t EventsEnabled: " + eventsEnabled);
//            if (eventsEnabled) {
//                try {
//                    emitProposalDecisionMadeEvent(proposal);
//                } catch (JsonProcessingException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            if (decision.approved()) {
//                agendaService.createAgendaItem(headers.get("Authorization"), proposal);
//                c4PMetrics.getApproved().increment();
//            }else{
//                c4PMetrics.getRejected().increment();
//            }
//
//            // Notify Potential Speaker By Email
//            emailService.notifySpeakerByEmail(headers.get("Authorization"), decision, proposal);
//
//
//        } else {
//
//            log.error(">> Proposal Not Found (" + id + ")");
//        }
//    }
//
//
    private Mono<Proposal> emitCloudEventForProposalAdded(Proposal proposal) {
        if (eventsEnabled) {
            CloudEventBuilder cloudEventBuilder = CloudEventBuilder.v1()
                    .withId(UUID.randomUUID().toString())
                    .withType("C4P.ProposalReceived")
                    .withSource(URI.create("c4p-service.default.svc.cluster.local"))
                    .withData(writeValueAsString(proposal).getBytes(StandardCharsets.UTF_8))
                    .withDataContentType("application/json; charset=UTF-8")
                    .withSubject(proposal.title());

            CloudEvent cloudEvent = cloudEventBuilder.build();

            logCloudEvent(cloudEvent);

            log.info("Producing CloudEvent with Proposal: " + proposal);

            webClient.baseUrl(K_SINK).filter(logRequest()).build()
                    .post().bodyValue(cloudEvent)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnError(t -> t.printStackTrace())
                    .doOnSuccess(s -> log.info("Result -> " + s)).subscribe();
        }
        return Mono.just(proposal);
    }

    private String writeValueAsString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Error when serializing Score", ex);
        }
    }

    private Proposal emitProposalDecisionMadeEvent(Proposal proposal) {
        log.info("Emitting Cloud Event with proposal " + proposal);
        if (eventsEnabled) {
            CloudEventBuilder cloudEventBuilder = CloudEventBuilder.v1()
                    .withId(UUID.randomUUID().toString())
                    .withType("C4P.ProposalDecisionMade")
                    .withSource(URI.create("c4p-service.default.svc.cluster.local"))
                    .withData(writeValueAsString(proposal).getBytes(StandardCharsets.UTF_8))
                    .withDataContentType("application/json; charset=UTF-8")
                    .withSubject(proposal.title());

            CloudEvent cloudEvent = cloudEventBuilder.build();

            logCloudEvent(cloudEvent);

            log.info("Producing CloudEvent with Proposal: " + proposal);

            webClient.baseUrl(K_SINK).filter(logRequest()).build()
                    .post().bodyValue(cloudEvent)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnError(t -> t.printStackTrace())
                    .doOnSuccess(s -> log.info("Result -> " + s)).subscribe();
        }
        return proposal;
    }

    private void logCloudEvent(CloudEvent cloudEvent) {
        EventFormat format = EventFormatProvider
                .getInstance()
                .resolveFormat(JsonFormat.CONTENT_TYPE);

        log.info("Cloud Event: " + new String(format.serialize(cloudEvent)));

    }

    //
    public static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info("Request: " + clientRequest.method() + " - " + clientRequest.url());
            clientRequest.headers().forEach((name, values) -> values.forEach(value -> log.info(name + "=" + value)));
            return Mono.just(clientRequest);
        });
    }

}
