package dev.loganalysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class IntelligentLogAnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntelligentLogAnalyzerApplication.class, args);
    }
}
