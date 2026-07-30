package dev.loganalysis.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.loganalysis.TestcontainersConfiguration;
import dev.loganalysis.importing.LogImportWorker;
import dev.loganalysis.persistence.repository.LogEventRepository;
import dev.loganalysis.persistence.repository.LogImportRepository;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Tag("integration")
@SpringBootTest(
        properties = {
            "analysis.imports.worker-enabled=true",
            "analysis.imports.initial-delay-ms=3600000",
            "analysis.imports.storage-path=${java.io.tmpdir}/intelligent-log-analyzer-tests"
        })
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LogIngestionApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private LogImportWorker importWorker;
    @Autowired private LogEventRepository eventRepository;
    @Autowired private LogImportRepository importRepository;

    @BeforeEach
    void cleanDatabase() {
        eventRepository.deleteAll();
        importRepository.deleteAll();
    }

    @Test
    void ingestsBatchWithPartialRejectionAndRequestId() throws Exception {
        String request =
                """
        {
          "format": "AUTO",
          "source": "api-1",
          "service": "payments",
          "entries": [
            {"line": "2026-07-30T10:00:00Z ERROR event_type=payment.failed order=17"},
            {"line": "not a supported line"}
          ]
        }
        """;

        mockMvc.perform(
                        post("/api/v1/log-events")
                                .header("X-Request-ID", "request-test-17")
                                .contentType("application/json")
                                .content(request))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Request-ID", "request-test-17"))
                .andExpect(jsonPath("$.acceptedCount").value(1))
                .andExpect(jsonPath("$.rejectedCount").value(1))
                .andExpect(jsonPath("$.rejections[0].code").value("unknown_format"));

        assertThat(eventRepository.count()).isEqualTo(1);
    }

    @Test
    void importsFileAsynchronouslyAndReportsSafeRejectionSample() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "device.log",
                        "text/plain",
                        ("<11>1 2026-07-30T10:01:00Z edge-7 device-agent 81 AUTH - "
                                        + "Authentication failed\ninvalid line\n")
                                .getBytes(StandardCharsets.UTF_8));

        String response =
                mockMvc.perform(
                                multipart("/api/v1/log-imports")
                                        .file(file)
                                        .param("format", "AUTO")
                                        .param("source", "edge-7")
                                        .param("service", "device-agent"))
                        .andExpect(status().isAccepted())
                        .andExpect(jsonPath("$.status").value("QUEUED"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        JsonNode submitted = objectMapper.readTree(response);
        String importId = submitted.get("id").asString();

        assertThat(importWorker.processNext()).isTrue();

        mockMvc.perform(get("/api/v1/log-imports/{id}", importId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.totalLines").value(2))
                .andExpect(jsonPath("$.acceptedLines").value(1))
                .andExpect(jsonPath("$.rejectedLines").value(1))
                .andExpect(jsonPath("$.rejectionSamples[0].code").value("unknown_format"));
    }
}
