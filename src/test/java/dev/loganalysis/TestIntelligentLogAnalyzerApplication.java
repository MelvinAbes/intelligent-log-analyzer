package dev.loganalysis;

import org.springframework.boot.SpringApplication;

public class TestIntelligentLogAnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.from(IntelligentLogAnalyzerApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
