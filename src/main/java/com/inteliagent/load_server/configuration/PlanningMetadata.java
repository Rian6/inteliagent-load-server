package com.inteliagent.load_server.configuration;
import java.util.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
/** Creator identity is assigned by the authenticated server, never by form input. */
public final class PlanningMetadata {
 private PlanningMetadata(){}
 public static String prepare(NamedParameterJdbcTemplate db,String id,String raw,boolean creating){
  try {
   var mapper=new tools.jackson.databind.json.JsonMapper();
   Map<String,Object> result=raw==null?new LinkedHashMap<>():mapper.readValue(raw,Map.class);
   if(!creating){
    String stored=db.queryForObject("SELECT BOLETIM_JSON FROM planejamento WHERE ID=:id",Map.of("id",id),String.class);
    Map<String,Object> previous=stored==null?Map.of():mapper.readValue(stored,Map.class);
    if(raw==null)result=new LinkedHashMap<>(previous);
    for(String key:List.of("criadorId","criadorNome","supervisorNome")){result.remove(key);if(previous.containsKey(key))result.put(key,previous.get(key));}
   }else{
    String name=db.queryForObject("SELECT NOME FROM usuario WHERE ID=:id AND ID_CEDE=:cede",Map.of("id",CurrentUser.id(),"cede",CurrentUser.cede()),String.class);
    result.put("criadorId",CurrentUser.id());result.put("criadorNome",name);result.put("supervisorNome",name);
   }
   for(String key:List.of("concluida","municipioIbge","categoriaLocalidade","atividade","tipoLocalidade"))result.remove(key);
   return mapper.writeValueAsString(result);
  }catch(Exception error){throw new IllegalArgumentException("Dados da localidade inválidos");}
 }
}
