package com.salaboy.conferences.c4p.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AgendaItem(Proposal proposal, String title, String author, String day, String time) {

}
