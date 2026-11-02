package com.inteliagent.load_server.controller.http;
import java.util.*;
import com.inteliagent.load_server.entity.*;
import com.inteliagent.load_server.service.SyncService;
import com.inteliagent.load_server.service.SyncDownloadService;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api")
public class SyncController {
    private final SyncService service;
    private final SyncDownloadService downloads;
    public SyncController(SyncService service, SyncDownloadService downloads) { this.service = service; this.downloads = downloads; }
    @PostMapping("/sync") public ConfirmLoads sync(@RequestBody List<Sync> batch) { return service.sync(batch); }
    @GetMapping("/consumer/{idUsuario}") public Map<String, Object> consumer(@PathVariable int idUsuario, @RequestParam(defaultValue="false") boolean isLoadFull, @RequestParam(defaultValue="0") int offset, @RequestParam(defaultValue="") String downloadId, @RequestParam(defaultValue="00000000-0000-0000-0000-000000000000") String clientId) { return downloads.consumer(idUsuario, isLoadFull, offset, downloadId, clientId); }
    @PostMapping("/consumer-confirm/{idUsuario}") public void confirm(@PathVariable int idUsuario, @RequestParam String downloadId, @RequestBody List<String> tables) { downloads.consumerConfirm(tables, downloadId, idUsuario); }
}
