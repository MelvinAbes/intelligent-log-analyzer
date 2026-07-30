package dev.loganalysis.api;

import dev.loganalysis.importing.InvalidImportException;
import dev.loganalysis.importing.LogImportNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidImportException.class)
    ProblemDetail invalidImport(InvalidImportException error, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, error.code(), error.getMessage(), request);
    }

    @ExceptionHandler(LogImportNotFoundException.class)
    ProblemDetail importNotFound(LogImportNotFoundException error, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "log_import_not_found", error.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException error, HttpServletRequest request) {
        ProblemDetail problem =
                problem(
                        HttpStatus.BAD_REQUEST,
                        "request_validation_failed",
                        "Request validation failed.",
                        request);
        Map<String, String> fields = new java.util.LinkedHashMap<>();
        error.getBindingResult()
                .getFieldErrors()
                .forEach(field -> fields.putIfAbsent(field.getField(), field.getDefaultMessage()));
        problem.setProperty("fields", fields);
        return problem;
    }

    private static ProblemDetail problem(
            HttpStatus status, String code, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setType(URI.create("urn:log-analysis:problem:" + code));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        Object requestId = request.getAttribute(RequestCorrelationFilter.REQUEST_ID_ATTRIBUTE);
        if (requestId != null) {
            problem.setProperty("requestId", requestId.toString());
        }
        return problem;
    }
}
