package dev.loganalysis.query;

import dev.loganalysis.event.domain.LogEvent;
import java.util.List;

public record LogSearchPage(
        List<LogEvent> items, int page, int size, long totalElements, int totalPages) {

    public LogSearchPage {
        items = List.copyOf(items);
    }
}
