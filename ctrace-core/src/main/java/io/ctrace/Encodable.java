package io.ctrace;

import java.util.Map;

public interface Encodable {

  String traceId();

  String spanId();

  String parentId();

  String service();

  String operation();

  long startMillis();

  long finishMillis();

  long duration();

  Iterable<Map.Entry<String, Object>> tags();

  LogEntry log();

  Iterable<LogEntry> logs();

  Iterable<Map.Entry<String, String>> baggage();

  String prefix();

  void setPrefix(String prefix);
}
