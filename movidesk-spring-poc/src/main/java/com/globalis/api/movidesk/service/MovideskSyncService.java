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
import java.util.ArrayList;
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
    
    // Filtro OData igual ao JS (Somente Tickets "AD FEIRAS - INFORMA")
    private final String FILTER = "customFieldValues/any(c: c/customFieldId eq 208535 and c/items/any(i: startswith(i/customFieldItem, 'AD FEIRAS - INFORMA')))";

    public void syncSingleTicket(Integer ticketId) {
        log.info("▶️ [MODO TESTE] Buscando APENAS o Ticket ID: {}", ticketId);
        MovideskResponseDTO loadedTicket = null;
        try {
            loadedTicket = restClient.get()
                    .uri(baseUrl + "?token={token}&id={id}&$expand=owner,actions,customFieldValues",
                            token, ticketId)
                    .retrieve()
                    .body(MovideskResponseDTO.class);
        } catch (Exception e) {
            log.error("❌ Falha na comunicação com a API do Movidesk para o Ticket {}: {}", ticketId, e.getMessage());
        }

        if (loadedTicket != null) {
            try {
                processAndSaveTicket(loadedTicket);
                log.info("🎉 Sucesso Absoluto! O Ticket {} e todas as suas ações foram salvas no MySQL do seu Workbench/DBeaver!", ticketId);
            } catch(Exception e) {
                log.error("❌ O Ticket foi baixado, mas houve um erro interno do Banco de Dados ao salvar o Ticket {}: {}", ticketId, e.getMessage());
            }
        }
    }

    public void syncAllTickets() {
        log.info("▶️ Iniciando Sincronização do Movidesk no Spring Boot...");

        List<Integer> allTicketIds = new ArrayList<>();
        int skip = 0;
        int top = 1000;
        boolean hasMore = true;

        // FASE 1: Buscar Todos os IDs Paginação
        while (hasMore) {
            log.info("📡 Buscando lista de Tickets na API (Lote: skip={})...", skip);
            try {
                MovideskResponseDTO[] listResponse = restClient.get()
                        .uri(baseUrl + "?token={token}&$filter={filter}&$select=id&$top={top}&$skip={skip}",
                                token, FILTER, top, skip)
                        .retrieve()
                        .body(MovideskResponseDTO[].class);

                if (listResponse == null || listResponse.length == 0) {
                    hasMore = false;
                } else {
                    for (MovideskResponseDTO t : listResponse) {
                        allTicketIds.add(t.id());
                    }
                    if (listResponse.length < top) {
                        hasMore = false;
                    } else {
                        skip += top;
                    }
                }
            } catch (Exception e) {
                log.error("❌ Erro grave ao listar IDs na API do Movidesk: {}", e.getMessage());
                return; // Aborta para evitar desastre
            }
        }

        log.info("✅ Total Encontrado: {} Tickets. Baixando CADA Ticket individualmente por segurança das Actions...", allTicketIds.size());

        // FASE 2: Buscar Detalhes de Cada Ticket 
        int progress = 1;
        for (Integer ticketId : allTicketIds) {
            log.info("⏳ Processando Ticket [{}/{}] -> ID: {}", progress++, allTicketIds.size(), ticketId);
            
            MovideskResponseDTO loadedTicket = null;
            int retries = 3;
            for (int i = 0; i < retries; i++) {
                try {
                    loadedTicket = restClient.get()
                            .uri(baseUrl + "?token={token}&id={id}&$expand=owner,actions,customFieldValues",
                                    token, ticketId)
                            .retrieve()
                            .body(MovideskResponseDTO.class);
                    break; // Sai do loop de Retry com sucesso
                } catch (Exception e) {
                    log.warn("⚠️ Timeout/Falha no Ticket {}, Restam {} tentativas. [{}]", ticketId, retries - 1 - i, e.getMessage());
                    try { Thread.sleep(3000); } catch (InterruptedException ignore) {}
                }
            }

            if (loadedTicket != null) {
                try {
                    processAndSaveTicket(loadedTicket);
                } catch(Exception e) {
                    log.error("❌ Erro interno de Banco ao salvar Ticket {}: {}", ticketId, e.getMessage());
                }
            } else {
                log.error("❌ Ticket {} escapou das {} tentativas na API e será pulado.", ticketId, retries);
            }
        }
        
        log.info("🎉 Sincronização Spring Boot FINALIZADA 100%!");
    }

    // A anotação @Transactional garante que erros em actions darão Rollback no Ticket inteiro
    @Transactional 
    public void processAndSaveTicket(MovideskResponseDTO dto) {
        
        // 1. Owner
        MovideskPerson owner = null;
        if (dto.owner() != null && dto.owner().id() != null) {
            owner = new MovideskPerson();
            owner.setId(dto.owner().id());
            owner.setBusinessName(dto.owner().businessName() != null ? dto.owner().businessName() : dto.owner().name());
            owner.setEmail(dto.owner().email());
            owner.setPhone(dto.owner().phone());
            owner.setPersonType(dto.owner().personType());
            owner.setProfileType(dto.owner().profileType());
            personRepository.save(owner);
        }

        // 2. Creator & Reference (Mesma Lógica de Desnormalização para B.I.)
        MovideskPerson creator = null;
        String clientRef = null;
        String orgBusinessName = null;
        
        if (dto.createdBy() != null && dto.createdBy().id() != null) {
            creator = new MovideskPerson();
            creator.setId(dto.createdBy().id());
            creator.setBusinessName(dto.createdBy().businessName() != null ? dto.createdBy().businessName() : dto.createdBy().name());
            creator.setEmail(dto.createdBy().email());
            creator.setPhone(dto.createdBy().phone());
            creator.setPersonType(dto.createdBy().personType());
            creator.setProfileType(dto.createdBy().profileType());
            personRepository.save(creator);
        }
        
        if (dto.clients() != null && !dto.clients().isEmpty()) {
            MovideskResponseDTO.ClientDTO firstClient = dto.clients().get(0);
            clientRef = firstClient.reference();
            if (firstClient.organization() != null) {
                orgBusinessName = firstClient.organization().businessName();
            }
            // Opcional: Persistir os contatos adicionais também como fizemos no JS
            for(MovideskResponseDTO.ClientDTO clientDto : dto.clients()) {
                if (clientDto.id() != null) {
                    MovideskPerson c = new MovideskPerson();
                    c.setId(clientDto.id());
                    c.setBusinessName(clientDto.businessName() != null ? clientDto.businessName() : clientDto.name());
                    c.setEmail(clientDto.email());
                    c.setPhone(clientDto.phone());
                    c.setPersonType(clientDto.personType());
                    c.setProfileType(clientDto.profileType());
                    personRepository.save(c);
                }
            }
        }

        // 3. Ticket
        MovideskTicket ticket = new MovideskTicket();
        ticket.setId(dto.id());
        ticket.setSubject(dto.subject());
        ticket.setCategory(dto.category());
        ticket.setStatus(dto.status());
        ticket.setBaseStatus(dto.baseStatus());
        ticket.setOwner(owner);
        ticket.setCreatedBy(creator);
        ticket.setClientReference(clientRef);
        ticket.setOrganizationBusinessName(orgBusinessName);
        ticket.setServiceFirstLevel(dto.serviceFirstLevel());
        ticket.setServiceSecondLevel(dto.serviceSecondLevel());
        ticket.setServiceThirdLevel(dto.serviceThirdLevel());
        ticket.setActionCount(dto.actionCount());
        ticket.setLifeTimeWorkingTime(dto.lifetimeWorkingTime());
        
        if (dto.createdDate() != null) {
            try { ticket.setCreatedDate(LocalDateTime.parse(dto.createdDate(), formatter)); } catch(Exception e){}
        }
        if (dto.resolvedIn() != null) {
            try { ticket.setResolvedIn(LocalDateTime.parse(dto.resolvedIn(), formatter)); } catch(Exception e){}
        }
        ticketRepository.save(ticket);

        // 4. Actions
        if (dto.actions() != null) {
            for (MovideskResponseDTO.ActionDTO actDto : dto.actions()) {
                MovideskAction action = new MovideskAction();
                action.setTicketId(ticket.getId());
                action.setId(actDto.id());
                action.setDescription(actDto.description());
                action.setType(actDto.type());
                action.setOrigin(actDto.origin());
                action.setStatus(actDto.status());
                action.setJustification(actDto.justification());
                
                MovideskPerson pAction = null;
                if (actDto.createdBy() != null && actDto.createdBy().id() != null) {
                    pAction = new MovideskPerson();
                    pAction.setId(actDto.createdBy().id());
                    pAction.setBusinessName(actDto.createdBy().businessName() != null ? actDto.createdBy().businessName() : actDto.createdBy().name());
                    personRepository.save(pAction);
                }
                action.setCreatedBy(pAction);

                if (actDto.createdDate() != null) {
                    try { action.setCreatedDate(LocalDateTime.parse(actDto.createdDate(), formatter)); } catch(Exception e){}
                }
                actionRepository.save(action);
            }
        }

        // 5. Custom Fields
        if (dto.customFieldValues() != null) {
            for (MovideskResponseDTO.CustomFieldValueDTO fieldDto : dto.customFieldValues()) {
                String valText = fieldDto.value();
                if (fieldDto.items() != null && !fieldDto.items().isEmpty()) {
                    List<String> items = fieldDto.items().stream()
                            .map(MovideskResponseDTO.CustomFieldItemDTO::customFieldItem)
                            .toList();
                    valText = String.join(", ", items);
                }
                
                MovideskCustomFieldValue customVal = new MovideskCustomFieldValue();
                customVal.setTicketId(ticket.getId());
                customVal.setCustomFieldId(fieldDto.customFieldId());
                customVal.setValText(valText);
                customFieldValueRepository.save(customVal);
                
                if (!definitionRepository.existsById(fieldDto.customFieldId())) {
                    MovideskCustomFieldDefinition def = new MovideskCustomFieldDefinition();
                    def.setId(fieldDto.customFieldId());
                    def.setName("Desconhecido Automático");
                    definitionRepository.save(def);
                }
            }
        }

        // 6. Tags
        if (dto.tags() != null) {
            for (String tagStr : dto.tags()) {
                MovideskTicketTag tag = new MovideskTicketTag();
                tag.setTicketId(ticket.getId());
                tag.setTag(tagStr);
                ticketTagRepository.save(tag);
            }
        }
    }
}
