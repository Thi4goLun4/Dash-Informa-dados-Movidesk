package com.globalis.api.movidesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.globalis.api.movidesk.model.MovideskTicket;

@Repository
public interface MovideskTicketRepository extends JpaRepository<MovideskTicket, Integer> {
}
