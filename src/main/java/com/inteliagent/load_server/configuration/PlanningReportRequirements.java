package com.inteliagent.load_server.configuration;
public final class PlanningReportRequirements {
 private PlanningReportRequirements(){}
 public static void validate(java.util.Map<String,Object> value){
  com.inteliagent.load_server.catalog.PlanningCatalog.validate(com.inteliagent.load_server.catalog.PlanningCatalog.VERSION,code(value,"ID_CIDADE"),code(value,"ID_CATEGORIA"),code(value,"ATIVIDADE"),code(value,"TIPO"),code(value,"CICLO"));
  if(value.get("CICLO")==null)throw new IllegalArgumentException("Informe o ciclo para o boletim");
  for(String key:java.util.List.of("NOME","ZONA","ANO"))if(value.get(key)==null||String.valueOf(value.get(key)).isBlank())throw new IllegalArgumentException("Preencha nome, zona e ano do planejamento");
  java.time.LocalDate.parse(String.valueOf(value.get("ANO")));
  Object raw=value.get("BOLETIM_JSON");if(!(raw instanceof String))throw new IllegalArgumentException("Informe o codigo da localidade para o boletim");
  ReportMetadata.validate((String)raw);
  try{
   var node=new tools.jackson.databind.json.JsonMapper().readTree((String)raw);
   var locality=node.get("codigoLocalidade");if(locality==null||!locality.isString()||locality.asString().isBlank()||locality.asString().trim().length()>32)throw new IllegalArgumentException();
   for(String[] pair:new String[][]{{"municipioIbge","ID_CIDADE"},{"atividade","ATIVIDADE"},{"tipoLocalidade","TIPO"}}){var field=node.get(pair[0]);if(field!=null&&!field.isNull()&&(!field.isString()||!field.asString().isBlank())&&(field.isNumber()?field.asDouble():Integer.parseInt(field.asString()))!=code(value,pair[1]))throw new IllegalArgumentException();}
   var category=node.get("categoriaLocalidade");if(category!=null&&!category.isNull()&&!category.asString().isBlank()&&!category.asString().equals(code(value,"ID_CATEGORIA")==10001?"BIR":"PV"))throw new IllegalArgumentException();
  }catch(Exception e){throw new IllegalArgumentException("Preencha o codigo da localidade e use a mesma classificacao do planejamento no boletim");}
 }
 private static Integer code(java.util.Map<String,Object> values,String key){Object raw=values.get(key);if(raw==null)return null;if(raw instanceof Number n&&n.doubleValue()==n.intValue())return n.intValue();throw new IllegalArgumentException("Codigo invalido");}
}
