package com.inteliagent.load_server.service;
import java.util.*;
import static com.inteliagent.load_server.service.SyncSchema.*;
import com.inteliagent.load_server.entity.*;
import com.inteliagent.load_server.configuration.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.security.access.AccessDeniedException;

@Service
public class SyncService {
    private final NamedParameterJdbcTemplate db;
    public SyncService(NamedParameterJdbcTemplate db) { this.db = db; }

    @Transactional(timeout=15)
    public ConfirmLoads sync(List<Sync> batch) {
        if (batch == null || batch.size() > 500) throw new IllegalArgumentException("Máximo 500 registros");
        batch.forEach(SyncSchema::validate);
        var preliminaryConflicts = new ArrayList<ConfirmLoads.Conflict>();
        for (Sync item : batch) {
            if (item == null || item.tabela()==null || item.values()==null) throw new IllegalArgumentException("Registro inválido");
            String table=item.tabela().toUpperCase(Locale.ROOT);
            var columns=COLUMNS.get(table);
            if(columns==null || !columns.containsAll(item.values().keySet()) || item.mutationId()==null) throw new IllegalArgumentException("Contrato de sincronização inválido");
            UUID.fromString(item.mutationId()); UUID.fromString(item.id());
            var args=new MapSqlParameterSource("id",item.id()).addValue("cede",CurrentUser.cede()).addValue("mutation",item.mutationId());
            var current=db.queryForList("SELECT sync_version FROM "+table+" WHERE ID=:id FOR UPDATE",args);
            if(!current.isEmpty() && db.queryForObject("SELECT COUNT(*) FROM "+table+" t WHERE t.ID=:id AND "+scope(table,"t"),args,Integer.class)!=1)throw new AccessDeniedException("Registro inacessível");
            if(db.queryForObject("SELECT COUNT(*) FROM sync_mutations WHERE mutation_id=:mutation",args,Integer.class)>0)continue;
            long version=current.isEmpty()?0:((Number)current.getFirst().get("sync_version")).longValue();
            if((version>0 && (item.baseVersion()==null || item.baseVersion()!=version)) || (version==0 && item.baseVersion()!=null && item.baseVersion()!=0)) {
                String fields=String.join(",",new TreeSet<>(columns));
                Map<String,Object> server=version==0?new HashMap<>(Map.of("ID",item.id(),"SITUACAO",0)):db.queryForMap("SELECT "+fields+" FROM "+table+" WHERE ID=:id",args);
                preliminaryConflicts.add(new ConfirmLoads.Conflict(item.mutationId(),table,item.id(),version,server));
            }
        }
        if(!preliminaryConflicts.isEmpty())throw new SyncConflictException(preliminaryConflicts);
        var confirmed = new ArrayList<String>();
        var applied = new ArrayList<ConfirmLoads.Applied>();
        var conflicts = new ArrayList<ConfirmLoads.Conflict>();
        for (Sync item : batch) {
            if (item == null || item.tabela() == null || item.values() == null) throw new IllegalArgumentException("Registro inválido");
            String table = item.tabela().toUpperCase(Locale.ROOT);
            var columns = COLUMNS.get(table);
            if (columns == null || item.values().isEmpty() || !columns.containsAll(item.values().keySet())) throw new IllegalArgumentException("Tabela ou coluna inválida");
            UUID.fromString(item.id());
            UUID.fromString(item.mutationId());
            if (item.baseVersion() != null && item.baseVersion() < 0) throw new IllegalArgumentException("Versão inválida");
            var values = new LinkedHashMap<String, Object>(item.values());
            if (!item.id().equals(values.get("ID"))) throw new IllegalArgumentException("ID divergente");
            for (Object value : values.values()) if (value != null && !(value instanceof String || value instanceof Number || value instanceof Boolean) || value instanceof String text && text.length() > 250000) throw new IllegalArgumentException("Valor inválido");
            var params = new MapSqlParameterSource("id", item.id()).addValue("cede", CurrentUser.cede());
            int existing = db.queryForObject("SELECT COUNT(*) FROM " + table + " t WHERE t.ID=:id", params, Integer.class);
            if (existing > 0 && db.queryForObject("SELECT COUNT(*) FROM " + table + " t WHERE t.ID=:id AND " + scope(table, "t"), params, Integer.class) != 1) throw new AccessDeniedException("Registro inacessível");
            if (table.equals("PLANEJAMENTO")) {
                if (!com.inteliagent.load_server.configuration.PlanningPermission.allowed(db,existing>0)) throw new AccessDeniedException("Sem permissão para este planejamento");
                if(existing==0)com.inteliagent.load_server.configuration.PlanningReportRequirements.validate(values);
                values.put("ID_CEDE", CurrentUser.cede());
            } else {
                String parent = table.equals("VISITA") ? "PLANEJAMENTO" : "VISITA";
                String foreignKey = "ID_" + parent;
                Object parentId = values.get(foreignKey);
                if (parentId == null || db.queryForObject("SELECT COUNT(*) FROM " + parent + " t WHERE t.ID=:parent AND " + scope(parent, "t"), params.addValue("parent", parentId), Integer.class) != 1) throw new AccessDeniedException("Vínculo inacessível");
                if (existing > 0 && db.queryForObject("SELECT COUNT(*) FROM " + table + " t WHERE t.ID=:id AND " + foreignKey + "=:parent", params, Integer.class) != 1) throw new AccessDeniedException("Vínculo imutável");
            }
            byte[] fingerprint;
            try {
                String canonical = new tools.jackson.databind.json.JsonMapper().writeValueAsString(new TreeMap<>(values)) + ":" + item.baseVersion();
                fingerprint = java.security.MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } catch (Exception e) { throw new IllegalArgumentException("Registro inválido"); }
            var ledgerParams = new MapSqlParameterSource("mutation",item.mutationId()).addValue("user",CurrentUser.id()).addValue("table",table).addValue("id",item.id()).addValue("hash",fingerprint);
            int reserved = db.update("INSERT IGNORE INTO sync_mutations (mutation_id,user_id,table_name,entity_id,payload_hash,applied_version) VALUES (:mutation,:user,:table,:id,:hash,0)",ledgerParams);
            if (reserved == 0) {
                var ledger = db.queryForMap("SELECT user_id,table_name,entity_id,payload_hash,applied_version FROM sync_mutations WHERE mutation_id=:mutation FOR UPDATE",ledgerParams);
                if (((Number)ledger.get("user_id")).intValue()!=CurrentUser.id() || !table.equals(ledger.get("table_name")) || !item.id().equals(ledger.get("entity_id")) || !java.security.MessageDigest.isEqual(fingerprint,(byte[])ledger.get("payload_hash"))) throw new IllegalArgumentException("Identificador de operação reutilizado");
                applied.add(new ConfirmLoads.Applied(item.mutationId(),((Number)ledger.get("applied_version")).longValue()));
                confirmed.add(table+":"+item.id());
                continue;
            }
            if(table.equals("PLANEJAMENTO"))values.put("BOLETIM_JSON",com.inteliagent.load_server.configuration.PlanningMetadata.prepare(db,item.id(),(String)values.get("BOLETIM_JSON"),existing==0));
            if(table.equals("VISITA")) {
                boolean canReorder=com.inteliagent.load_server.configuration.VisitPermission.allowed(db,true);
                var parentParams=new MapSqlParameterSource("id",values.get("ID_PLANEJAMENTO")).addValue("cede",CurrentUser.cede());
                db.queryForList("SELECT ID FROM planejamento WHERE ID=:id AND ID_CEDE=:cede FOR UPDATE",parentParams);
                if(existing==0) {
                    if(!com.inteliagent.load_server.configuration.VisitPermission.allowed(db,false))throw new AccessDeniedException("Sem permissão para adicionar visitas");
                    if(!canReorder || values.get("SEQUENCIA")==null)values.put("SEQUENCIA",db.queryForObject("SELECT COALESCE(MAX(SEQUENCIA),0)+1 FROM visita WHERE ID_PLANEJAMENTO=:id",parentParams,Integer.class));
                } else if(values.containsKey("SEQUENCIA") && !canReorder) {
                    Number sequence=db.queryForObject("SELECT SEQUENCIA FROM visita WHERE ID=:id",params,Number.class);
                    Object requested=values.get("SEQUENCIA");
                    if(!(sequence==null && requested==null) && !(sequence!=null && requested instanceof Number n && sequence.doubleValue()==n.doubleValue()))throw new AccessDeniedException("Sem permissão para alterar a ordem das visitas");
                }
            }
            var current = db.queryForList("SELECT sync_version FROM " + table + " WHERE ID=:id FOR UPDATE",params);
            long version = current.isEmpty()?0:((Number)current.getFirst().get("sync_version")).longValue();
            if ((version > 0 && (item.baseVersion()==null || item.baseVersion()!=version)) || (version==0 && item.baseVersion()!=null && item.baseVersion()!=0)) {
                String fields=String.join(",",new TreeSet<>(columns));
                Map<String,Object> serverValues=version==0?new HashMap<>(Map.of("ID",item.id(),"SITUACAO",0)):db.queryForMap("SELECT "+fields+" FROM "+table+" WHERE ID=:id",params);
                conflicts.add(new ConfirmLoads.Conflict(item.mutationId(),table,item.id(),version,serverValues));
                db.update("DELETE FROM sync_mutations WHERE mutation_id=:mutation",ledgerParams);
                throw new SyncConflictException(conflicts);
            }
            values.put("sync_version",version+1);
            var keys = new ArrayList<>(values.keySet());
            String names = String.join(",", keys);
            String placeholders = String.join(",", keys.stream().map(k -> ":" + k).toList());
            String updates = String.join(",", keys.stream().filter(k -> !k.equals("ID")).map(k -> k + "=:" + k).toList());
            if (updates.isEmpty()) throw new IllegalArgumentException("Registro vazio");
            db.update("INSERT INTO " + table + " (" + names + ") VALUES (" + placeholders + ") ON DUPLICATE KEY UPDATE " + updates, values);
            confirmed.add(table + ":" + item.id());
            db.update("UPDATE sync_mutations SET applied_version=:version WHERE mutation_id=:mutation",ledgerParams.addValue("version",version+1));
            applied.add(new ConfirmLoads.Applied(item.mutationId(),version+1));
        }
        var result = new ConfirmLoads(); result.setConfirmedIds(confirmed); result.setApplied(applied); result.setConflicts(conflicts); return result;
    }
}
