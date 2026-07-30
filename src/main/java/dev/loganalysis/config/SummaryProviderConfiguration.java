package dev.loganalysis.config;

import dev.loganalysis.incident.summary.DisabledIncidentSummaryProvider;
import dev.loganalysis.incident.summary.HttpChatSummaryProvider;
import dev.loganalysis.incident.summary.IncidentSummaryProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class SummaryProviderConfiguration {

    @Bean
    IncidentSummaryProvider incidentSummaryProvider(AnalysisProperties properties) {
        var settings = properties.summary();
        return switch (settings.provider().strip().toLowerCase(java.util.Locale.ROOT)) {
            case "disabled" -> new DisabledIncidentSummaryProvider();
            case "chat-http" -> {
                var requestFactory = new JdkClientHttpRequestFactory();
                requestFactory.setReadTimeout(settings.timeout());
                yield new HttpChatSummaryProvider(
                        RestClient.builder().requestFactory(requestFactory), settings);
            }
            default ->
                    throw new IllegalStateException(
                            "Unsupported summary provider: " + settings.provider());
        };
    }
}
