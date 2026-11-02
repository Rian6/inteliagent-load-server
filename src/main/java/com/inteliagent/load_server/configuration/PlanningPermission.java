package com.inteliagent.load_server.configuration;
public final class PlanningPermission {
 private PlanningPermission(){}
 public static boolean allowed(org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate db,boolean existing){
 String column=existing?"PODE_EDITAR_PLANEJAMENTO":"PODE_CRIAR_PLANEJAMENTO";
 return Boolean.TRUE.equals(db.queryForObject("SELECT "+column+" FROM USUARIO WHERE ID=:id AND ID_CEDE=:cede AND SITUACAO=1 AND SITUACAO_APROVACAO=1",java.util.Map.of("id",CurrentUser.id(),"cede",CurrentUser.cede()),Boolean.class));
 }
}
