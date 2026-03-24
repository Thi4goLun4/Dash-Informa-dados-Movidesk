package com.globalis.api.movidesk.controller;

import com.globalis.api.movidesk.service.MovideskSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movidesk")
@RequiredArgsConstructor
public class MovideskSyncController {

    private final MovideskSyncService syncService;

    @GetMapping("/sync")
    public ResponseEntity<String> triggerSync() {
        syncService.syncAllTickets();
        return ResponseEntity.ok("▶️ Processo de sincronização COM TODOS OS TICKETS iniciado em BackGround!");
    }

    @GetMapping("/sync/{id}")
    public ResponseEntity<String> triggerSingleSync(@PathVariable Integer id) {
        syncService.syncSingleTicket(id);
        return ResponseEntity.ok("▶️ Teste Unitário Disparado! Vá olhar o terminal do Java (VS Code) para ver o bloco azul salvando o Ticket " + id + " no seu MySQL do DBeaver! 🏆");
    }
}
