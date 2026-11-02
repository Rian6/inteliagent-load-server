package com.inteliagent.load_server;

import com.inteliagent.load_server.entity.Sync;
import com.inteliagent.load_server.service.SyncService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SyncSecurityTests {
    @Test void untrustedTableAndColumnNamesNeverReachDatabase() {
        var db = mock(NamedParameterJdbcTemplate.class);
        var service = new SyncService(db);
        assertThrows(IllegalArgumentException.class, () -> service.sync(List.of(new Sync("id", "usuario", Map.of("senha", "x")))));
        assertThrows(IllegalArgumentException.class, () -> service.sync(List.of(new Sync("id", "VISITA; DROP TABLE usuario", Map.of("ID", "id")))));
        assertThrows(IllegalArgumentException.class, () -> service.sync(List.of(new Sync("id", "VISITA", Map.of("ID; DELETE", "id")))));
        verifyNoInteractions(db);
    }
    @Test void oversizedBatchesAreRejectedBeforeWriting() {
        var db = mock(NamedParameterJdbcTemplate.class);
        var service = new SyncService(db);
        assertThrows(IllegalArgumentException.class, () -> service.sync(java.util.Collections.nCopies(501, new Sync("id", "VISITA", Map.of()))));
        verifyNoInteractions(db);
    }
}
