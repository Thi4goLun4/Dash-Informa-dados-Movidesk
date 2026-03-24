package com.globalis.api.movidesk.controller;

import com.globalis.api.movidesk.service.MovideskSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
        return ResponseEntity.ok("▶️ Processo de sincronização com o Movidesk iniciado com sucesso em BackGround no servidor Java!");
    }
}
