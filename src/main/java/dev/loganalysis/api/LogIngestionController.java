package dev.loganalysis.api;

import dev.loganalysis.api.model.IngestLogBatchRequest;
import dev.loganalysis.api.model.IngestLogBatchResponse;
import dev.loganalysis.api.model.LogImportResponse;
import dev.loganalysis.event.domain.LogFormat;
import dev.loganalysis.importing.LogImportService;
import dev.loganalysis.ingestion.LogEventIngestionService;
import dev.loganalysis.ingestion.NumberedLogLine;
import dev.loganalysis.parsing.ParseHints;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class LogIngestionController {

    private final LogEventIngestionService ingestionService;
    private final LogImportService importService;

    public LogIngestionController(
            LogEventIngestionService ingestionService, LogImportService importService) {
        this.ingestionService = ingestionService;
        this.importService = importService;
    }

    @PostMapping("/log-events")
    public ResponseEntity<IngestLogBatchResponse> ingest(
            @Valid @RequestBody IngestLogBatchRequest request) {
        List<NumberedLogLine> lines =
                java.util.stream.IntStream.range(0, request.entries().size())
                        .mapToObj(
                                index ->
                                        new NumberedLogLine(
                                                index + 1, request.entries().get(index).line()))
                        .toList();
        var result =
                ingestionService.ingest(
                        lines,
                        request.format(),
                        new ParseHints(request.source(), request.service()),
                        null);
        return ResponseEntity.created(URI.create("/api/v1/log-events"))
                .body(IngestLogBatchResponse.from(result));
    }

    @PostMapping(path = "/log-imports", consumes = "multipart/form-data")
    public ResponseEntity<LogImportResponse> submitImport(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "AUTO") LogFormat format,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String service) {
        var logImport = importService.submit(file, format, source, service);
        return ResponseEntity.accepted()
                .location(URI.create("/api/v1/log-imports/" + logImport.getId()))
                .body(LogImportResponse.from(logImport));
    }

    @GetMapping("/log-imports/{id}")
    public LogImportResponse getImport(@PathVariable UUID id) {
        return LogImportResponse.from(importService.get(id));
    }

    @GetMapping("/log-imports")
    public List<LogImportResponse> listImports() {
        return importService.list().stream().map(LogImportResponse::from).toList();
    }
}
