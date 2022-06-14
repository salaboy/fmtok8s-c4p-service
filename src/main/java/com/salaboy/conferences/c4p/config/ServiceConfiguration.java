package com.salaboy.conferences.c4p.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
@ConfigurationProperties(prefix = "app.services.config")
public class ServiceConfiguration {
    private URI email;
    private URI agenda;
    private URI broker;
    private boolean enabled;

    public void setEmail(URI email) {
        this.email = email;
    }

    public void setAgenda(URI agenda) {
        this.agenda = agenda;
    }

    public void setBroker(URI broker) {
        this.broker = broker;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public URI getEmail() {
        return email;
    }

    public URI getAgenda() {
        return agenda;
    }

    public URI getBroker() {
        return broker;
    }

    public boolean isEnabled() {
        return enabled;
    }
}

