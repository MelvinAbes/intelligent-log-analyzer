package dev.loganalysis.query;

import dev.loganalysis.persistence.entity.LogEventEntity;
import dev.loganalysis.persistence.repository.LogEventRepository;
import java.time.Instant;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogSearchService {

    private static final Instant MINIMUM_TIME = Instant.parse("1970-01-01T00:00:00Z");
    private static final Instant MAXIMUM_TIME = Instant.parse("9999-12-31T23:59:59Z");

    private final LogEventRepository repository;

    public LogSearchService(LogEventRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public LogSearchPage search(LogSearchCriteria criteria) {
        validate(criteria);
        var page =
                repository.search(
                        criteria.from() == null ? MINIMUM_TIME : criteria.from(),
                        criteria.to() == null ? MAXIMUM_TIME : criteria.to(),
                        normalize(criteria.source()),
                        normalize(criteria.service()),
                        criteria.severity() == null ? "" : criteria.severity().name(),
                        normalize(criteria.query()),
                        PageRequest.of(criteria.page(), criteria.size()));
        return new LogSearchPage(
                page.stream().map(LogEventEntity::toDomain).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    private static void validate(LogSearchCriteria criteria) {
        if (criteria.page() < 0) {
            throw new InvalidQueryException("Page must not be negative.");
        }
        if (criteria.size() < 1 || criteria.size() > 100) {
            throw new InvalidQueryException("Page size must be between 1 and 100.");
        }
        if (criteria.from() != null
                && criteria.to() != null
                && !criteria.from().isBefore(criteria.to())) {
            throw new InvalidQueryException("Search start must be before its end.");
        }
        if (criteria.query() != null && criteria.query().length() > 200) {
            throw new InvalidQueryException("Full-text query must not exceed 200 characters.");
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? "" : value.strip();
    }
}
