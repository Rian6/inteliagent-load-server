package com.inteliagent.load_server.controller.http;


import com.inteliagent.load_server.entity.ConfirmLoads;
import com.inteliagent.load_server.entity.Consumer;
import com.inteliagent.load_server.entity.Sync;
import com.inteliagent.load_server.service.SyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SyncController {

    @Autowired
    private SyncService syncService;

    @PostMapping("/sync")
    public ResponseEntity<?> sync(@RequestBody List<Sync> syncList) {
        try {
            ConfirmLoads confirmLoads = syncService.sync(syncList);
            return ResponseEntity.created(new URI("")).body(confirmLoads);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao syncronizar cargas: " + e.getMessage());
        }
    }
    @GetMapping("/consumer/{idUsuario}")
    public ResponseEntity<?> consumer(
            @PathVariable int idUsuario,
            @RequestParam Boolean isLoadFull) {
        try {
            Consumer consumerEntities = syncService.consumer(idUsuario, isLoadFull);
            return ResponseEntity.created(new URI("")).body(consumerEntities);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao sincronizar cargas: " + e.getMessage());
        }
    }
    @PostMapping("/consumer-confirm/{idUsuario}")
    public ResponseEntity<?> consumerConfirm(
            @PathVariable int idUsuario,
            @RequestParam String dataHora,
            @RequestBody List<String> tables) {
        try {
            syncService.consumerConfirm(tables, dataHora, idUsuario);
            return ResponseEntity.ok("");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao sincronizar cargas: " + e.getMessage());
        }
    }
}
