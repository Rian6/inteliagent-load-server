package com.inteliagent.load_server.entity;
import java.util.Map;
public record Sync(String id, String tabela, Map<String, Object> values, String mutationId, Long baseVersion) {
    public Sync(String id, String tabela, Map<String, Object> values) { this(id,tabela,values,null,null); }
}
