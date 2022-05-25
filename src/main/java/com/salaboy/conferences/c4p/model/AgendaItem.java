package com.salaboy.conferences.c4p.model;

public class AgendaItem {

    private Proposal proposal;
    private String title;
    private String author;
    private String day;
    private String time;


    public AgendaItem(Proposal proposal, String title, String author, String day, String time) {
        this.proposal = proposal;
        this.title = title;
        this.author = author;
        this.day = day;
        this.time = time;
    }

    public AgendaItem() {
    }

    public Proposal getProposal() {
        return proposal;
    }

    public void setProposal(Proposal proposal) {
        this.proposal = proposal;
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
