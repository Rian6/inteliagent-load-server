package com.inteliagent.load_server.service;

import java.util.*;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import com.inteliagent.load_server.configuration.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import static com.inteliagent.load_server.service.SyncSchema.*;

/** Read/download boundary. Cursor state is independent from upload transactions. */
@Service
public class SyncDownloadService {
    private final NamedParameterJdbcTemplate db;
    private final Clock clock;
    private record Download(int user, String clientId, LocalDateTime until, Map<String, LocalDateTime> since, Set<String> complete, java.util.concurrent.atomic.AtomicInteger nextOffset, Map<Integer,Map<String,String>> cursors, long expires) {}
    private final java.util.concurrent.ConcurrentHashMap<String, Download> downloads = new java.util.concurrent.ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    public SyncDownloadService(NamedParameterJdbcTemplate db) { this(db, Clock.systemUTC()); }
    SyncDownloadService(NamedParameterJdbcTemplate db, Clock clock) { this.db = db; this.clock = clock; }
    @Transactional(readOnly = true)
    public Map<String, Object> consumer(int user, boolean full, int offset, String downloadId, String clientId) {
        if (user != CurrentUser.id()) throw new AccessDeniedException("Usuário divergente");
        if (downloadId == null || clientId == null) throw new IllegalArgumentException("Download inválido");
        UUID.fromString(clientId);
        if (offset < 0 || offset > 1000000) throw new IllegalArgumentException("Página inválida");
        downloads.entrySet().removeIf(e -> e.getValue().expires() < clock.millis());
        if (downloadId.isEmpty()) {
            if (offset != 0 || downloads.size() >= 1000) throw new IllegalArgumentException("Reinicie o download");
            downloadId = UUID.randomUUID().toString();
            var since = new HashMap<String, LocalDateTime>();
            for (String table : COLUMNS.keySet()) {
                var prior = db.queryForList("SELECT ULTIMO_SINCRONISMO FROM sync_confirm WHERE ID_USUARIO=:user AND SYNC_TABLE=:table AND CLIENT_ID=:client", Map.of("user",user,"table",table,"client",clientId));
                LocalDateTime time = LocalDateTime.of(2000,1,1,0,0);
                if (!full && !prior.isEmpty() && prior.getFirst().get("ULTIMO_SINCRONISMO") instanceof Timestamp value) time = value.toLocalDateTime().minusSeconds(30);
                since.put(table,time);
            }
            LocalDateTime until = db.queryForObject("SELECT NOW(6)", Map.of(), LocalDateTime.class);
            downloads.put(downloadId,new Download(user,clientId,until,since,java.util.concurrent.ConcurrentHashMap.newKeySet(),new java.util.concurrent.atomic.AtomicInteger(0),new java.util.concurrent.ConcurrentHashMap<>(Map.of(0,Map.of())),clock.millis()+300_000));
        }
        Download download = downloads.get(downloadId);
        if (download == null || download.user() != user || !download.clientId().equals(clientId) || !download.cursors().containsKey(offset)) throw new IllegalArgumentException("Download expirado");
        var loads = new ArrayList<Map<String, Object>>();
        var nextCursors = new HashMap<String,String>();
        for (String table : List.of("PLANEJAMENTO", "VISITA", "INSPECAO", "AMOSTRA", "TRATAMENTO")) {
            // Photos stay with the sample aggregate; keep download pages bounded.
            int limit=table.equals("AMOSTRA")?10:500;
            var fields = new TreeSet<>(COLUMNS.get(table));
            String projection = String.join(",", fields.stream().map(k -> "t." + k + " AS " + k).toList()) + ",t.sync_version AS __version";
            var rows = db.queryForList("SELECT " + projection + " FROM " + table + " t WHERE " + scope(table,"t") + " AND t.updated_at >= :since AND t.updated_at <= :until AND t.ID > :after ORDER BY t.ID LIMIT " + (limit+1), Map.of("cede", CurrentUser.cede(), "after", download.cursors().getOrDefault(offset,Map.of()).getOrDefault(table,""), "since", download.since().get(table), "until", download.until()));
            if (rows.size() <= limit) download.complete().add(table);
            nextCursors.put(table,rows.isEmpty()?download.cursors().getOrDefault(offset,Map.of()).getOrDefault(table,""):String.valueOf(rows.get(Math.min(limit-1,rows.size()-1)).get("ID")));
            loads.add(Map.of("tableName", table, "rows", rows.stream().limit(limit).toList(), "hasMore", rows.size() > limit));
        }
        download.cursors().putIfAbsent(offset+500,nextCursors);
        download.nextOffset().compareAndSet(offset,offset+500);
        return Map.of("consumerLoadsList", loads, "offset", offset, "nextOffset", offset + 500, "downloadId", downloadId);
    }
    @Transactional
    public void consumerConfirm(List<String> tables, String downloadId, int user) {
        if (user != CurrentUser.id()) throw new AccessDeniedException("Usuário divergente");
        Download download = downloads.get(downloadId);
        if (tables == null || download == null || download.user() != user || download.expires() < clock.millis() || !download.complete().containsAll(COLUMNS.keySet()) || !new HashSet<>(tables).equals(COLUMNS.keySet())) throw new IllegalArgumentException("Download incompleto ou expirado");
        for (String table : COLUMNS.keySet()) db.update("INSERT INTO sync_confirm (ULTIMO_SINCRONISMO,SYNC_TABLE,ID_USUARIO,CLIENT_ID) VALUES (:time,:table,:user,:client) ON DUPLICATE KEY UPDATE ULTIMO_SINCRONISMO=GREATEST(COALESCE(ULTIMO_SINCRONISMO,:time),:time)", Map.of("time",download.until(),"table",table,"user",user,"client",download.clientId()));
        downloads.remove(downloadId);
    }
}
