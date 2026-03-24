package com.globalis.api.movidesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.globalis.api.movidesk.model.MovideskTicketTag;

@Repository
public interface MovideskTicketTagRepository extends JpaRepository<MovideskTicketTag, MovideskTicketTag.MovideskTicketTagId> {
}
