package com.salaboy.conferences.c4p.rest;

import com.salaboy.conferences.c4p.rest.model.Proposal;
import com.salaboy.conferences.c4p.rest.model.ProposalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProposalRepository extends JpaRepository<Proposal, String> {

    List<Proposal> findAllByStatus(final ProposalStatus proposalStatus);

}