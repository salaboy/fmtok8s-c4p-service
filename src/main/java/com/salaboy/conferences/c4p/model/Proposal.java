package com.salaboy.conferences.c4p.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "proposals")
public record Proposal(@Id
                       Long id,
                       String title,
                       String description,
                       String author,
                       String email,
                       boolean approved,
                       ProposalStatus status) {

    @PersistenceCreator
    public Proposal(
            Long id, String title, String description, String author, String email, boolean approved, ProposalStatus status) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.author = author;
        this.email = email;
        this.approved = approved;
        if (status != null) {
            this.status = status;
        } else {
            this.status = ProposalStatus.PENDING;
        }
    }

    public Proposal(String title, String description, String author, String email) {
        this(null, title, description, author, email, false, ProposalStatus.PENDING);
    }

    public Proposal approve() {
        return new Proposal(id(), title(), description(), author(), email(), true, ProposalStatus.DECIDED);
    }

    public Proposal reject() {
        return new Proposal(id(), title(), description(), author(), email(), false, ProposalStatus.DECIDED);
    }

}
