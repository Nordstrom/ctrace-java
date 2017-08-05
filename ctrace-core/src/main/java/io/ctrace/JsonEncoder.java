package io.ctrace;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Encodes span into array of bytes in JSON UTF-8 format.
 */
public class JsonEncoder implements Encoder {

  private static void encodePrefix(StringBuilder builder, Encodable e) {
    String prefix = e.prefix();
    if (prefix != null) {
      builder.append(prefix);
      return;
    }

    builder.append("{\"traceId\":\"")
        .append(e.traceId())
        .append("\",\"spanId\":\"")
        .append(e.spanId())
        .append("\",");

    String parentId = e.parentId();
    if (parentId != null) {
      builder.append("\"parentId\":\"")
          .append(parentId)
          .append("\",");
    }

    String serviceName = e.service();
    if (serviceName != null) {
      builder.append("\"service\":\"")
          .append(serviceName)
          .append("\",");
    }
    builder.append("\"operation\":\"")
        .append(e.operation())
        .append("\",\"start\":")
        .append(e.startMillis());

    e.setPrefix(builder.toString());
  }

  private static void encodeFinish(StringBuilder builder, Encodable e) {
    long finish = e.finishMillis();
    if (finish > 0) {
      builder.append(",\"finish\":")
          .append(finish)
          .append(",\"duration\":")
          .append(e.duration());
    }
  }

  private static void encodeTags(StringBuilder builder, Encodable e) {
    Iterable<Map.Entry<String, Object>> tags = e.tags();
    if (tags == null) {
      return;
    }
    builder.append(",\"tags\":{");
    boolean first = true;
    for (Map.Entry<String, ?> entry : tags) {
      if (first) {
        first = false;
      } else {
        builder.append(',');
      }
      builder.append('"')
          .append(entry.getKey())
          .append("\":");
      Object value = entry.getValue();
      boolean quote = value instanceof String;
      if (quote) {
        builder.append('"');
      }

      builder.append(value);
      if (quote) {
        builder.append('"');
      }
    }
    builder.append("}");
  }

  private static void encodeBaggage(StringBuilder builder, Encodable e) {
    Iterable<Map.Entry<String, String>> baggage = e.baggage();
    if (baggage == null) {
      return;
    }

    builder.append(",\"baggage\":{");
    boolean first = true;
    for (Map.Entry<String, String> entry : baggage) {
      if (first) {
        first = false;
      } else {
        builder.append(',');
      }
      builder.append('"')
          .append(entry.getKey())
          .append("\":\"")
          .append(entry.getValue())
          .append('"');
    }
    builder.append("}");
  }

  private static void encodeLogs(StringBuilder builder, Encodable e) {
    LogEntry log = e.log();
    if (log != null) {
      // Multi Event:  Only one log to encode.  Then return
      builder.append(",\"log\":");
      encodeLog(builder, log);
      return;
    }

    // Single Event:  Multiple logs to encode.
    Iterable<LogEntry> logs = e.logs();
    if (logs == null) {
      return;
    }
    builder.append(",\"logs\":[");
    boolean first = true;
    for (LogEntry entry : logs) {
      if (first) {
        first = false;
      } else {
        builder.append(',');
      }
      encodeLog(builder, entry);
    }
    builder.append(']');
  }

  private static void encodeLog(StringBuilder builder, LogEntry entry) {
    builder.append("{\"timestamp\":")
        .append(entry.timestampMillis());

    Iterable<? extends Map.Entry<String, ?>> fields = entry.fields();
    for (Map.Entry<String, ?> field : fields) {
      builder.append(",\"")
          .append(field.getKey())
          .append("\":");
      Object value = field.getValue();
      boolean quote = value instanceof String;
      if (quote) {
        builder.append('"');
      }
      builder.append(value);
      if (quote) {
        builder.append('"');
      }
    }
    builder.append('}');
  }

  private static void encodeSuffix(StringBuilder builder) {
    builder.append('}')
        .append(System.lineSeparator());
  }

  /**
   * Encode a span into an array of bytes in JSON UTF-8 format.
   *
   * @param e - span data to encode
   * @return array of encoded bytes.
   */
  @Override
  public String encodeToString(Encodable e) {
    StringBuilder builder = new StringBuilder();

    encodePrefix(builder, e);
    encodeFinish(builder, e);
    encodeTags(builder, e);
    encodeBaggage(builder, e);
    encodeLogs(builder, e);
    encodeSuffix(builder);
    return builder.toString();
  }

  @Override
  public byte[] encodeToBytes(Encodable e) {
    return this.encodeToString(e)
        .getBytes(StandardCharsets.UTF_8);
  }
}
