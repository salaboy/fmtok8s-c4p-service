package com.salaboy.conferences.c4p.rest.model;

import java.util.Date;

public class AgendaItem {

    private String proposalId;
    private String title;
    private String author;
    private String day;
    private String time;


    public AgendaItem(String proposalId, String title, String author, String day, String time) {
        this.proposalId = proposalId;
        this.title = title;
        this.author = author;
        this.day = day;
        this.time = time;
        this.time = time;
    }


    public AgendaItem() {
    }

    public String getProposalId() {
        return proposalId;
    }

    public void setProposalId(String proposalId) {
        this.proposalId = proposalId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
