package com.globalis.api.movidesk.service;

import com.globalis.api.movidesk.dto.MovideskResponseDTO;
import com.globalis.api.movidesk.model.*;
import com.globalis.api.movidesk.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovideskSyncService {

    private final MovideskTicketRepository ticketRepository;
    private final MovideskPersonRepository personRepository;
    private final MovideskActionRepository actionRepository;
    private final MovideskCustomFieldValueRepository customFieldValueRepository;
    private final MovideskTicketTagRepository ticketTagRepository;
    private final MovideskCustomFieldDefinitionRepository definitionRepository;

    @Value("${movidesk.api.token}")
    private String token;

    @Value("${movidesk.api.base-url}")
    private String baseUrl;

    private final RestClient restClient = RestClient.create();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS");

    @Transactional
    public void syncTickets() {
        log.info("Iniciando Sincronização de Tickets do Movidesk...");

        // Simulando a lógica de listagem (Paginação omitida para brevidade no POC)
        // Onde traríamos a lista de IDs baseada na query OData.
        
        // Exemplo simplificado puxando 1 ticket usando os DTOs para Upsert:
        try {
            MovideskResponseDTO ticketDto = restClient.get()
                    .uri(baseUrl + "?token={token}&id=1071897&$expand=owner,actions,customFieldValues", token)
                    .retrieve()
                    .body(MovideskResponseDTO.class);

            if (ticketDto != null) {
                processAndSaveTicket(ticketDto);
            }
            
        } catch(Exception e) {
            log.error("Erro ao sincronizar: ", e);
        }
    }

    private void processAndSaveTicket(MovideskResponseDTO dto) {
        // 1. Salvar Owner
        MovideskPerson owner = null;
        if (dto.owner() != null) {
            owner = new MovideskPerson();
            owner.setId(dto.owner().id());
            // Preencher demais campos conforme DTO...
            personRepository.save(owner);
        }

        // 2. Salvar Ticket Principal
        MovideskTicket ticket = new MovideskTicket();
        ticket.setId(dto.id());
        ticket.setSubject(dto.subject());
        ticket.setOwner(owner);
        // Preencher outros campos e o createdBy...
        ticketRepository.save(ticket);

        // 3. Salvar Ações
        if (dto.actions() != null) {
            for (MovideskResponseDTO.ActionDTO actDto : dto.actions()) {
                MovideskAction action = new MovideskAction();
                action.setTicketId(ticket.getId());
                action.setId(actDto.id());
                action.setDescription(actDto.description());
                actionRepository.save(action);
            }
        }
        
        log.info("Ticket {} processado e salvo no banco de dados.", ticket.getId());
    }
}
