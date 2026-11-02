package com.inteliagent.load_server.entity;

import java.util.List;

public class ConfirmLoads {
    public record Applied(String mutationId, long version) {}
    public record Conflict(String mutationId, String table, String id, long serverVersion, java.util.Map<String,Object> serverValues) {}
    private List<Applied> applied = java.util.List.of();
    private List<Conflict> conflicts = java.util.List.of();
    public List<Applied> getApplied() { return applied; }
    public void setApplied(List<Applied> value) { applied = value; }
    public List<Conflict> getConflicts() { return conflicts; }
    public void setConflicts(List<Conflict> value) { conflicts = value; }
    private List<String> confirmedIds;

    public List<String> getConfirmedIds() {
        return confirmedIds;
    }

    public void setConfirmedIds(List<String> confirmedIds) {
        this.confirmedIds = confirmedIds;
    }
}
