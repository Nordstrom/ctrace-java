package io.ctrace;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Encodes span into array of bytes in JSON UTF-8 format.
 */
public class JsonEncoder implements Encoder {
    /**
     * Encode a span into an array of bytes in JSON UTF-8 format.
     * @param span - span to encode
     * @return array of encoded bytes.
     */
    @Override
    public byte[] encode(Span span) {
        StringBuilder builder = new StringBuilder();

        encodePrefix(builder, span);
        encodeFinish(builder, span);
        encodeTags(builder, span);
        encodeBaggage(builder, span);
        encodeLogs(builder, span);
        encodeSuffix(builder);
        return builder.toString()
                      .getBytes(StandardCharsets.UTF_8);
    }

    private static void encodePrefix(StringBuilder builder, Span span) {
        String prefix = span.encodedPrefix();
        if (prefix != null) {
            builder.append(prefix);
            return;
        }

        builder.append("{\"traceId\":\"")
               .append(span.traceId())
               .append("\",\"spanId\":\"")
               .append(span.spanId())
               .append("\",");

        String parentId = span.parentId();
        if (parentId != null) {
            builder.append("\"parentId\":\"")
                   .append(parentId)
                   .append("\",");
        }

        String serviceName = span.serviceName();
        if (serviceName != null) {
            builder.append("\"service\":\"")
                   .append(serviceName)
                   .append("\",");
        }
        builder.append("\"operation\":\"")
               .append(span.operationName())
               .append("\",\"start\":")
               .append(span.startMicros());

        span.setEncodedPrefix(builder.toString());
    }

    private static void encodeFinish(StringBuilder builder, Span span) {
        long finish = span.finishMicros();
        if (finish > 0) {
            builder.append(",\"finish\":")
                   .append(finish)
                   .append(",\"duration\":")
                   .append(span.duration());
        }
    }

    private static void encodeTags(StringBuilder builder, Span span) {
        Iterable<Map.Entry<String, Object>> tags = span.tagEntries();
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

    private static void encodeBaggage(StringBuilder builder, Span span) {
        Iterable<Map.Entry<String, String>> baggage = span.baggageItems();
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

    private static void encodeLogs(StringBuilder builder, Span span) {
        LogEntry log = span.log();
        if (log != null) {
            // Multi Event:  Only one log to encode.  Then return
            builder.append(",\"log\":");
            encodeLog(builder, log);
            return;
        }

        // Single Event:  Multiple logs to encode.
        Iterable<LogEntry> logs = span.logEntries();
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
               .append(entry.timestampMicros());

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
}
