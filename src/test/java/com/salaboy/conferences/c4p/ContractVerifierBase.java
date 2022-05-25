package com.salaboy.conferences.c4p;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "server.port=0")
@ActiveProfiles("dev")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class ContractVerifierBase {

    @LocalServerPort
    int port;


    @BeforeAll
    public void setup() {
        MockitoAnnotations.initMocks(this);
        RestAssured.baseURI = "http://localhost:" + this.port;
    }
}
