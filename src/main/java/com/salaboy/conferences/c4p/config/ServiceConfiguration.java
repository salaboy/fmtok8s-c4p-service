package com.salaboy.conferences.c4p.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
@ConfigurationProperties(prefix = "c4p")
public class ServiceConfiguration {
    private URI emailService;
    private URI agendaService;
    private URI brokerURL;
    private boolean brokerEnabled;

    public void setEmailService(URI emailService) {
        this.emailService = emailService;
    }

    public void setAgendaService(URI agendaService) {
        this.agendaService = agendaService;
    }

    public void setBrokerURL(URI brokerURL) {
        this.brokerURL = brokerURL;
    }

    public void setBrokerEnabled(boolean brokerEnabled) {
        this.brokerEnabled = brokerEnabled;
    }

    public URI getEmailService() {
        return emailService;
    }

    public URI getAgendaService() {
        return agendaService;
    }

    public URI getBrokerURL() {
        return brokerURL;
    }

    public boolean isBrokerEnabled() {
        return brokerEnabled;
    }
}

