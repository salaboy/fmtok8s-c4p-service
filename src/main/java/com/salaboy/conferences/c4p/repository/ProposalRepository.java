package com.salaboy.conferences.c4p.repository;

import com.salaboy.conferences.c4p.model.Proposal;
import com.salaboy.conferences.c4p.model.ProposalStatus;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ProposalRepository extends ReactiveCrudRepository<Proposal, Long> {

    Flux<Proposal> findAllByStatus(final ProposalStatus proposalStatus);

}