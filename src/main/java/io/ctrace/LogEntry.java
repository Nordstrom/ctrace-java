package io.ctrace;

import java.util.Map;

final class LogEntry {
    private final long timestampMicros;
    private final Map<String, ?> fields;

    LogEntry(long timestampMicros, Map<String, ?> fields) {
        this.timestampMicros = timestampMicros;
        this.fields = fields;
    }

    long timestampMicros() {
        return timestampMicros;
    }

    Iterable<? extends Map.Entry<String, ?>> fields() {
        return fields.entrySet();
    }
}

