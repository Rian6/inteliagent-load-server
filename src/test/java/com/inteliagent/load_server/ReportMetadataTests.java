package com.inteliagent.load_server;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.inteliagent.load_server.configuration.ReportMetadata;
class ReportMetadataTests {
 @Test void quantitiesAcceptNumericAndDecimalCommaValues(){
  assertDoesNotThrow(()->ReportMetadata.validate("{\"focalGramas\":\"2,5\",\"focal2\":{\"tipo\":\"BTI\",\"gramas\":1.5,\"depositos\":2}}"));
 }
 @Test void rejectsInvalidShapeTimeAndQuantities(){
  for(String value:java.util.List.of("[]","invalid","{\"horaEntrada\":\"25:00\"}","{\"focal2\":{\"gramas\":-1}}","{\"focal2\":{\"depositos\":1.5}}","{\"focalGramas\":\"NaN\"}")) assertThrows(IllegalArgumentException.class,()->ReportMetadata.validate(value));
 }
}
