package dev.loganalysis.query;

import dev.loganalysis.persistence.entity.IncidentEntity;
import java.util.List;

public record IncidentPage(
        List<IncidentEntity> items, int page, int size, long totalElements, int totalPages) {

    public IncidentPage {
        items = List.copyOf(items);
    }
}
