package com.salaboy.conferences.c4p;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salaboy.conferences.c4p.model.*;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = C4PServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "app.services.config.events.enabled=false")
@ActiveProfiles("dev")
public class C4PServiceApplicationTest {

    private static final int POSTGRESQL_PORT = 5432;
    private static MockWebServer mockWebServer;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void setup() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void clean() throws IOException {
        mockWebServer.shutdown();
    }

    @Autowired
    private WebTestClient webTestClient;

   

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("c4p.agendaService", () -> mockWebServer.url("/agenda/").uri());
        registry.add("c4p.emailService", () -> mockWebServer.url("/email/").uri());
        registry.add("spring.r2dbc.username", () -> "postgres");
    }


    @BeforeEach
    public void beforeAll() {
        deleteProposals();
    }

    private Proposal createProposal() {

        var requestProposal =
                new Proposal(null, "Title", "Description", "Author",
                        "email@email.com", false, ProposalStatus.PENDING);

        return webTestClient.post()
                .uri("/")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestProposal))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(Proposal.class)
                .returnResult()
                .getResponseBody();
    }

    private List<Proposal> getAllProposals() {

        return webTestClient.get()
                .uri("/")
                .exchange()
                .expectBodyList(Proposal.class)
                .returnResult()
                .getResponseBody();
    }

    @Test
    public void newProposal_ShouldBeCreateAProposal() {

        // action
        var proposal = createProposal();

        // assert
        assertThat(proposal.id()).isNotNull().isNotEqualTo(0L);
    }

    @Test
    public void deleteProposal_ShouldDeleteProposalById() {

        // arrange
        var proposal = createProposal();

        // action, assert
        webTestClient.delete()
                .uri("/" + proposal.id())
                .exchange()
                .expectStatus()
                .isOk();

        assertThatThereAreNotProposals();
    }

    @Test
    public void deleteNonExistentProposal_ShouldReturnNotFound() {

        var nonExistentProposalId = 99;
        // action, assert
        webTestClient.delete()
                .uri("/" + nonExistentProposalId)
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    public void deleteProposals_ShouldDeleteAllProposals() {

        // arrange
        createProposal();
        createProposal();

        // action, assert
        deleteProposals().expectStatus().isOk();

        assertThatThereAreNotProposals();
    }


    @Test
    public void decide_ShouldBeDecidedProposal() throws JsonProcessingException, InterruptedException {

        AgendaItem mockAgendaItem = new AgendaItem(new Proposal("title", "description", "author", "email"), "title", "author", "day", "time");
        Notification mockNotification = new Notification("id", "title", "to");

        final Dispatcher dispatcher = new Dispatcher() {

            @Override
            public MockResponse dispatch(RecordedRequest request) {

                switch (request.getPath()) {
                    case "/email/notification":
                        try {
                            return new MockResponse().setResponseCode(200).setBody(objectMapper.writeValueAsString(mockNotification))
                                    .addHeader("Content-Type", "application/json");
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    case "/agenda/":
                        try {
                            return new MockResponse().setResponseCode(200).setBody(objectMapper.writeValueAsString(mockAgendaItem))
                                    .addHeader("Content-Type", "application/json");
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                }
                return new MockResponse().setResponseCode(404);
            }
        };
        mockWebServer.setDispatcher(dispatcher);


        var proposal = createProposal();
        var decision = new ProposalDecision(true);

        webTestClient.post()
                .uri("/" + proposal.id() + "/decision")
                .body(BodyInserters.fromValue(decision))
                .exchange()
                .expectStatus()
                .isOk();


        var recordedRequest1 = mockWebServer.takeRequest();
        var recordedRequest2 = mockWebServer.takeRequest();
        List<RecordedRequest> requests = new ArrayList<>();
        requests.add(recordedRequest1);
        requests.add(recordedRequest2);


        assertThat(requests).isNotEmpty().hasSize(2);
        assertThat(requests).extracting(RecordedRequest::getRequestUrl)
                .containsExactlyInAnyOrder(HttpUrl.get(mockWebServer.url("/agenda/").uri()),
                                    HttpUrl.get(mockWebServer.url("/email/notification").uri()));

        //@TODO: check Body for requests
        //assertThat(requests).extracting(RecordedRequest::getBody).contains("/agenda/", "/email/notifications");

        getAllProposals().forEach(item -> {
            assertThat(item.status()).isEqualTo(ProposalStatus.DECIDED);
        });
    }

    private void assertThatThereAreNotProposals() {
        assertThat(getAllProposals()).hasSize(0);
    }

    private WebTestClient.ResponseSpec deleteProposals() {
        return webTestClient.delete()
                .uri("/")
                .exchange();
    }
}

