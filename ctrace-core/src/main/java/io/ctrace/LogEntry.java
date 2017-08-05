package io.ctrace;

import java.util.Map;

public final class LogEntry {

  private final long timestampMillis;
  private final Map<String, ?> fields;

  public LogEntry(long timestampMillis, Map<String, ?> fields) {
    this.timestampMillis = timestampMillis;
    this.fields = fields;
  }

  public long timestampMillis() {
    return this.timestampMillis;
  }

  public Iterable<? extends Map.Entry<String, ?>> fields() {
    return fields.entrySet();
  }
}

