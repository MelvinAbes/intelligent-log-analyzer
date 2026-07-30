package dev.loganalysis.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(
        name = "incident_events",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_incident_events_incident_event",
                        columnNames = {"incident_id", "event_id"}))
public class IncidentEventEntity {

    @Id private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private IncidentEntity incident;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private LogEventEntity event;

    @Column(nullable = false, length = 32)
    private String role;

    protected IncidentEventEntity() {}

    public IncidentEventEntity(IncidentEntity incident, LogEventEntity event, String role) {
        id = UUID.randomUUID();
        this.incident = incident;
        this.event = event;
        this.role = role;
    }
}
