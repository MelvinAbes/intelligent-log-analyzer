package dev.loganalysis.parsing;

import dev.loganalysis.event.domain.LogFormat;

public interface LogLineParser {

    LogFormat format();

    boolean supports(String line);

    ParsedLogLine parse(String line, ParseHints hints);
}
