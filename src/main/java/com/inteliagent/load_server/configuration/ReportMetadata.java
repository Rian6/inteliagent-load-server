package com.inteliagent.load_server.configuration;
/** Bounded, structured supplementary fields shared by web and offline sync. */
public final class ReportMetadata {
 private ReportMetadata() {}
 public static void validate(String value) {
  if(value==null)return;
  if(value.length()>10000)throw new IllegalArgumentException("Dados do boletim muito longos");
  try {
   var node=new tools.jackson.databind.json.JsonMapper().readTree(value);
   if(!node.isObject())throw new IllegalArgumentException();
   var hour=node.get("horaEntrada");
   if(hour!=null&&!hour.asString().isBlank()&&!hour.asString().matches("([01][0-9]|2[0-3]):[0-5][0-9]"))throw new IllegalArgumentException();
   var primary=node.get("focalGramas");
   if(primary!=null&&(!primary.isString()||!primary.asString().isBlank())){
    double n=primary.isNumber()?primary.asDouble():Double.parseDouble(primary.asString().replace(',','.'));
    if(!Double.isFinite(n)||n<0||n>1000000)throw new IllegalArgumentException();
   }
   var lab=node.get("laboratorio");
   if(lab!=null){
    if(!lab.isObject())throw new IllegalArgumentException();
    var completed=lab.get("concluido");
    if(completed!=null&&completed.asBoolean()){
     var entered=java.time.LocalDate.parse(lab.get("dataEntrada").asString());
     var finished=java.time.LocalDate.parse(lab.get("dataConclusao").asString());
     if(finished.isBefore(entered))throw new IllegalArgumentException();
     for(String key:java.util.List.of("nome","laboratorista"))if(lab.get(key)==null||lab.get(key).asString().isBlank()||lab.get(key).asString().length()>120)throw new IllegalArgumentException();
     for(String species:java.util.List.of("aegypti","albopictus","outras")){
      var result=lab.get(species);if(result==null)continue;if(!result.isObject())throw new IllegalArgumentException();
      var deposits=result.get("depositos");if(deposits!=null&&!deposits.isObject())throw new IllegalArgumentException();
      for(String key:java.util.List.of("A1","A2","B","C","D1","D2","E","larvas","pupas","exuvias","adultos")){
       var quantity=java.util.List.of("larvas","pupas","exuvias","adultos").contains(key)?result.get(key):deposits==null?null:deposits.get(key);
       if(quantity==null||quantity.isString()&&quantity.asString().isBlank())continue;
       double n=quantity.isNumber()?quantity.asDouble():Double.parseDouble(quantity.asString());
       if(!Double.isFinite(n)||n<0||n>1000000||n!=Math.floor(n))throw new IllegalArgumentException();
      }
     }
    }
   }
   var product=node.get("focal2");
   if(product!=null){
    if(!product.isObject())throw new IllegalArgumentException();
    for(String key:java.util.List.of("gramas","depositos")){
     var quantity=product.get(key);
     if(quantity!=null&&(!quantity.isString()||!quantity.asString().isBlank())){
      double n=quantity.isNumber()?quantity.asDouble():Double.parseDouble(quantity.asString().replace(',','.'));
      if(!Double.isFinite(n)||n<0||n>1000000||key.equals("depositos")&&n!=Math.floor(n))throw new IllegalArgumentException();
     }
    }
   }
  } catch(Exception e){throw new IllegalArgumentException("Dados do boletim invalidos");}
 }
}
