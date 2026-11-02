package com.inteliagent.load_server.configuration;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.EmptyResultDataAccessException;
@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(com.inteliagent.load_server.service.SyncConflictException.class)
    ResponseEntity<?> conflict(com.inteliagent.load_server.service.SyncConflictException e) { return ResponseEntity.status(409).body(java.util.Map.of("conflicts",e.getConflicts())); }
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> invalid(IllegalArgumentException e) { return problem(400, "Dados inválidos"); }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> forbidden(AccessDeniedException e) { return problem(403, "Acesso negado"); }
    @ExceptionHandler(EmptyResultDataAccessException.class)
    ResponseEntity<ProblemDetail> missing(EmptyResultDataAccessException e) { return problem(404, "Registro não encontrado"); }
    @ExceptionHandler({org.springframework.dao.DataIntegrityViolationException.class, org.springframework.http.converter.HttpMessageNotReadableException.class})
    ResponseEntity<ProblemDetail> invalidPayload(Exception e) { return problem(400, "Dados inválidos"); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> failure(Exception e) {
        org.slf4j.LoggerFactory.getLogger(getClass()).error("Request failed", e);
        return problem(500, "Não foi possível concluir a operação");
    }
    private ResponseEntity<ProblemDetail> problem(int status, String message) { return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(status), message)); }
}
