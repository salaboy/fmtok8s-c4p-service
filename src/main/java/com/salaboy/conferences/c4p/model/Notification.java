package com.salaboy.conferences.c4p.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Notification(String id, String title, String to) {
}
