package dev.loganalysis.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.loganalysis.TestcontainersConfiguration;
import dev.loganalysis.event.domain.EventSeverity;
import dev.loganalysis.event.domain.LogEvent;
import dev.loganalysis.event.domain.LogFormat;
import dev.loganalysis.incident.domain.IncidentSeverity;
import dev.loganalysis.persistence.entity.IncidentEntity;
import dev.loganalysis.persistence.entity.IncidentEventEntity;
import dev.loganalysis.persistence.entity.LogEventEntity;
import dev.loganalysis.persistence.entity.LogImportEntity;
import dev.loganalysis.persistence.repository.IncidentEventRepository;
import dev.loganalysis.persistence.repository.IncidentRepository;
import dev.loganalysis.persistence.repository.LogEventRepository;
import dev.loganalysis.persistence.repository.LogImportRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class PostgresPersistenceTest {

    @Autowired private LogImportRepository importRepository;
    @Autowired private LogEventRepository eventRepository;
    @Autowired private IncidentRepository incidentRepository;
    @Autowired private IncidentEventRepository incidentEventRepository;

    @Test
    void persistsImportEventIncidentAndTimelineLink() {
        Instant now = Instant.parse("2026-07-30T10:00:00Z");
        LogImportEntity logImport =
                importRepository.save(
                        LogImportEntity.queued(
                                "gateway.jsonl",
                                LogFormat.JSON_LINES,
                                "imports/gateway.jsonl",
                                "edge-1",
                                "gateway",
                                now));
        LogEvent event =
                new LogEvent(
                        UUID.randomUUID(),
                        logImport.getId(),
                        1,
                        now,
                        now.plusSeconds(1),
                        "edge-1",
                        "gateway",
                        EventSeverity.ERROR,
                        "connection.failed",
                        "Connection refused",
                        "trace-17",
                        null,
                        "event-17",
                        "7a5a63a36a",
                        Map.of("region", "eu-central"),
                        "sanitized raw line");
        LogEventEntity eventEntity =
                eventRepository.save(new LogEventEntity(event, logImport, event.lineNumber()));
        IncidentEntity incident =
                incidentRepository.save(
                        new IncidentEntity(
                                "repeated_error",
                                "gateway:7a5a63a36a",
                                IncidentSeverity.HIGH,
                                "Repeated gateway error",
                                "The gateway logged repeated connection failures.",
                                now,
                                now,
                                now,
                                now.plusSeconds(2),
                                1,
                                Map.of("observed_count", 1, "threshold", 1)));
        incidentEventRepository.save(new IncidentEventEntity(incident, eventEntity, "trigger"));

        assertThat(eventRepository.findById(event.id()).orElseThrow().toDomain())
                .satisfies(
                        stored -> {
                            assertThat(stored.service()).isEqualTo("gateway");
                            assertThat(stored.attributes()).containsEntry("region", "eu-central");
                        });
        assertThat(incidentEventRepository.count()).isEqualTo(1);
    }
}
