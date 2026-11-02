package com.inteliagent.load_server.service;
import com.inteliagent.load_server.entity.ConfirmLoads;
import java.util.List;
public class SyncConflictException extends RuntimeException {
    private final List<ConfirmLoads.Conflict> conflicts;
    public SyncConflictException(List<ConfirmLoads.Conflict> conflicts) { super("Edição concorrente"); this.conflicts=conflicts; }
    public List<ConfirmLoads.Conflict> getConflicts() { return conflicts; }
}
