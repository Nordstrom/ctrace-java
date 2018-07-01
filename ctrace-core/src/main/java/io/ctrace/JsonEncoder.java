package io.ctrace;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.val;

/** Encodes span into array of bytes in JSON UTF-8 format. */
public class JsonEncoder implements Encoder {

  private static void encodeStart(StringBuilder builder, Span span) {
    builder
        .append("{\"traceId\":\"")
        .append(span.context().traceId())
        .append("\",\"spanId\":\"")
        .append(span.context().spanId())
        .append("\",");

    String parentId = span.parentId();
    if (parentId != null) {
      builder.append("\"parentId\":\"").append(parentId).append("\",");
    }

    String serviceName = span.serviceName();
    if (serviceName != null) {
      builder.append("\"service\":\"").append(serviceName).append("\",");
    }
    builder
        .append("\"operation\":\"")
        .append(span.operationName())
        .append("\",\"start\":")
        .append(span.startMillis());
  }

  private static void encodeFinish(StringBuilder builder, Span span) {
    long finish = span.finishMillis();
    if (finish > 0) {
      builder
          .append(",\"finish\":")
          .append(finish)
          .append(",\"duration\":")
          .append(span.duration());
    }
  }

  private static void encodeTags(StringBuilder builder, Span span) {
    val tags = span.tags();
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
      builder.append('"').append(entry.getKey()).append("\":");
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

  private static void encodeBaggage(StringBuilder builder, Span span) {
    val baggage = span.baggage();
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
      builder
          .append('"')
          .append(entry.getKey())
          .append("\":\"")
          .append(entry.getValue())
          .append('"');
    }
    builder.append("}");
  }

  private static void encodeLog(StringBuilder builder, Log entry) {
    builder.append(",\"log\":{\"timestamp\":").append(entry.timestampMillis());

    val fields = entry.fields();
    for (val field : fields.entrySet()) {
      builder.append(",\"").append(field.getKey()).append("\":");
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
    builder.append('}').append(System.lineSeparator());
  }

  /**
   * Encode a span into an array of bytes in JSON UTF-8 format.
   *
   * @param span - span data to encode
   * @return array of encoded bytes.
   */
  @Override
  public String encodeToString(Span span, Log log) {
    StringBuilder builder = new StringBuilder();

    encodeStart(builder, span);
    encodeFinish(builder, span);
    encodeLog(builder, log);
    encodeTags(builder, span);
    encodeBaggage(builder, span);
    encodeSuffix(builder);
    return builder.toString();
  }

  @Override
  public byte[] encodeToBytes(Span span, Log log) {
    return this.encodeToString(span, log).getBytes(StandardCharsets.UTF_8);
  }
}
