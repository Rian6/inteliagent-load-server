package com.inteliagent.load_server.catalog;

import java.util.*;
import tools.jackson.databind.json.JsonMapper;

/** Reference snapshot bundled identically with both apps, so field work stays offline. */
public final class PlanningCatalog {
    public static final String VERSION="PNCD-FAD-IBGE-v1";
    private static final Set<Integer> MUNICIPALITIES=municipalities();
    private PlanningCatalog() {}
    private static Set<Integer> municipalities() {
        try(var stream=PlanningCatalog.class.getResourceAsStream("/catalogos/planning-catalog.json")) {
            if(stream==null) throw new IllegalStateException("Catálogo ausente");
            var root=new JsonMapper().readTree(stream);
            if(!VERSION.equals(root.get("version").asString())) throw new IllegalStateException("Versão divergente");
            Set<Integer> ids=new HashSet<>();
            for(var row:root.get("municipios")) ids.add(row.get("value").asInt());
            return Set.copyOf(ids);
        }catch(Exception error){throw new IllegalStateException("Não foi possível ler o catálogo",error);}
    }
    public static void validate(String version,Integer city,Integer category,Integer activity,Integer type,Integer cycle) {
        if(cycle!=null && (cycle<0||cycle>99)) throw new IllegalArgumentException("Ciclo inválido");
        // No migration guesses what codes in an unversioned legacy plan meant.
        if(version==null)return;
        if(!VERSION.equals(version)||city==null||!MUNICIPALITIES.contains(city)||category==null||!Set.of(10001,10002).contains(category)||activity==null||activity<1||activity>6||type==null||!Set.of(1,2).contains(type)) throw new IllegalArgumentException("Classificação inválida");
    }
    private static Integer code(Object value) {
        if(value==null)return null;
        if(value instanceof Number number && Double.isFinite(number.doubleValue()) && number.doubleValue()==number.intValue())return number.intValue();
        throw new IllegalArgumentException("Código inválido");
    }
    public static void validateValues(Map<String,Object> values) {
        Object version=values.get("CATALOGO_VERSAO");
        Integer cycle=code(values.get("CICLO"));
        if(version==null){validate(null,null,null,null,null,cycle);return;}
        if(!(version instanceof String text))throw new IllegalArgumentException("Versão inválida");
        validate(text,code(values.get("ID_CIDADE")),code(values.get("ID_CATEGORIA")),code(values.get("ATIVIDADE")),code(values.get("TIPO")),cycle);
    }
}
