package com.inteliagent.load_server.service;

import java.util.Map;
import java.util.Set;

/** Shared allowlist and tenant predicates for the synchronization protocol. */
final class SyncSchema {
    private SyncSchema() {}
    static void validate(com.inteliagent.load_server.entity.Sync item) {
        if (item == null || item.id() == null || item.tabela() == null || item.values() == null || item.mutationId() == null) {
            throw new IllegalArgumentException("Registro inválido");
        }
        if ("PLANEJAMENTO".equalsIgnoreCase(item.tabela())) {
            var v=item.values();
            com.inteliagent.load_server.catalog.PlanningCatalog.validateValues(v);
        }
        var columns = COLUMNS.get(item.tabela().toUpperCase(java.util.Locale.ROOT));
        if (columns == null || item.values().isEmpty() || !columns.containsAll(item.values().keySet())) {
            throw new IllegalArgumentException("Tabela ou coluna inválida");
        }
        java.util.UUID.fromString(item.id());
        java.util.UUID.fromString(item.mutationId());
        if (!item.id().equals(item.values().get("ID")) || item.baseVersion() != null && item.baseVersion() < 0) {
            throw new IllegalArgumentException("Identidade ou versão inválida");
        }
        for (var entry : item.values().entrySet()) {
            if (entry.getValue() instanceof String text && text.length() > (entry.getKey().equals("IMAGENS_JSON") ? 250000 : 10000)) throw new IllegalArgumentException("Valor muito longo");
        }
        Object metadata=item.values().get("BOLETIM_JSON");
        if(metadata!=null){if(!(metadata instanceof String))throw new IllegalArgumentException("Dados do boletim invalidos");com.inteliagent.load_server.configuration.ReportMetadata.validate((String)metadata);}
        Object photos = item.values().get("IMAGENS_JSON");
        if (photos instanceof String json && !json.isBlank()) {
            try {
                var list = new tools.jackson.databind.json.JsonMapper().readTree(json);
                if (!list.isArray() || list.size() > 2) throw new IllegalArgumentException("Fotos inválidas");
                for (var image : list) if (!image.isString() || image.asString().length() > 120000 || !image.asString().matches("data:image/jpeg;base64,[A-Za-z0-9+/]+={0,2}")) throw new IllegalArgumentException("Foto inválida");
            } catch (Exception error) { throw new IllegalArgumentException("Fotos inválidas"); }
        }
        for (Object value : item.values().values()) {
            if (value != null && !(value instanceof String || value instanceof Number || value instanceof Boolean)
                    || value instanceof String text && text.length() > 250000
                    || value instanceof Number number && !Double.isFinite(number.doubleValue())) {
                throw new IllegalArgumentException("Valor inválido");
            }
        }
    }
    static final Map<String, Set<String>> COLUMNS = Map.of(
        "PLANEJAMENTO", Set.of("ID", "ID_CIDADE", "ID_CATEGORIA", "NOME", "ATIVIDADE", "TIPO", "DATA_ULT_VISITA", "ANO", "ZONA", "STATUS", "CICLO", "CATALOGO_VERSAO", "BOLETIM_JSON", "SITUACAO"),
        "VISITA", Set.of("ID", "ID_PLANEJAMENTO", "PRIMEIRA_VISITA", "SEGUNDA_VISITA", "TIPO_VISITA", "NUMERO_QUARTOS", "CONTEM_AMOSTRA", "CONTEM_TRATAMENTO", "SEQUENCIA", "LATITUDE", "LONGITUDE", "SITUACAO_REFERENCIA", "CEP", "NOME", "NUMERO", "COMPLEMENTO", "LADO", "TIPO_IMOVEL", "BOLETIM_JSON", "SITUACAO"),
        "INSPECAO", Set.of("ID", "ID_VISITA", "TIPO", "NUMERO_DEPOSITOS", "SITUACAO"),
        "AMOSTRA", Set.of("ID", "ID_VISITA", "NUMERO_INICIO", "NUMERO_FINAL", "QUANTIDADE", "IMAGENS_JSON", "SITUACAO"),
        "TRATAMENTO", Set.of("ID", "ID_VISITA", "DEPOSITOS_ELIMINADOS", "IMOVEIS_TRATADOS", "QTD_TRATAMENTO", "TIPO", "QTD_CARGA", "SITUACAO"));
    static String scope(String table, String alias) {
        if (table.equals("PLANEJAMENTO")) return alias + ".id_cede = :cede";
        if (table.equals("VISITA")) return "EXISTS (SELECT 1 FROM planejamento p WHERE p.id = " + alias + ".ID_PLANEJAMENTO AND p.id_cede = :cede)";
        return "EXISTS (SELECT 1 FROM visita v JOIN planejamento p ON p.id=v.ID_PLANEJAMENTO WHERE v.id=" + alias + ".ID_VISITA AND p.id_cede=:cede)";
    }
}
