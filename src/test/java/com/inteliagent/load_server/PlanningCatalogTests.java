package com.inteliagent.load_server;
import com.inteliagent.load_server.catalog.PlanningCatalog;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PlanningCatalogTests {
    @Test void officialCodesAndZeroCycleAreAccepted() {
        assertDoesNotThrow(()->PlanningCatalog.validate(PlanningCatalog.VERSION,4104808,10001,2,1,0));
        assertDoesNotThrow(()->PlanningCatalog.validate(null,1,77,88,99,null));
    }
    @Test void invalidReferencesAndCycleAreRejected() {
        assertThrows(IllegalArgumentException.class,()->PlanningCatalog.validate(PlanningCatalog.VERSION,1,10001,2,1,0));
        assertThrows(IllegalArgumentException.class,()->PlanningCatalog.validate(PlanningCatalog.VERSION,4104808,77,2,1,0));
        assertThrows(IllegalArgumentException.class,()->PlanningCatalog.validate(PlanningCatalog.VERSION,4104808,10001,7,1,0));
        assertThrows(IllegalArgumentException.class,()->PlanningCatalog.validate(null,null,null,null,null,-1));
        assertThrows(IllegalArgumentException.class,()->PlanningCatalog.validateValues(Map.of("CICLO",1.5)));
    }
}
