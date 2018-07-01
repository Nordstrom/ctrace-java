package io.ctrace;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@Slf4j
public class Slf4jLogger implements Logger {
  private static final org.slf4j.Logger logger = LoggerFactory.getLogger("CTRACE");

  @Override
  public void start(Span span, Log log) {
    activate(span);
    log(span, log);
  }

  @Override
  public void activate(Span span) {
    MDC.clear();
    MDC.put("traceId", span.context().traceId());
    MDC.put("spanId", span.context().spanId());
    val parentId = span.parentId();
    if (parentId != null) {
      MDC.put("parentId", parentId);
    }
    MDC.put("service", span.serviceName());
    MDC.put("operation", span.operationName());
    MDC.put("start", Long.toString(span.startMillis()));
  }

  @Override
  public void finish(Span span, Log log) {
    MDC.put("finish", Long.toString(span.finishMillis()));
    MDC.put("duration", Long.toString(span.duration()));
    log(span, log);
  }

  @Override
  public void log(Span span, Log log) {
    putBag(span.baggage());
    putTags(span.tags());
    putLogContext(log);
    logger.info(log.message());
    clearLogContext(log);
  }

  private static void putBag(Iterable<Map.Entry<String, String>> bag) {
    if (bag == null) {
      return;
    }
    for (val item : bag) {
      MDC.put("baggage." + item.getKey(), item.getValue());
    }
  }

  private static void putTags(Iterable<? extends Map.Entry<String, ?>> tags) {
    if (tags == null) {
      return;
    }
    for (val tag : tags) {
      MDC.put("tags." + tag.getKey(), tag.getValue().toString());
    }
  }

  private static void putLogContext(Log log) {
    for (val field : log.fields().entrySet()) {
      if (field.getKey().equals(log.messageKey())) {
        continue;
      }
      MDC.put(field.getKey(), field.getValue().toString());
    }
  }

  private static void clearLogContext(Log log) {
    for (val field : log.fields().entrySet()) {
      if (field.getKey().equals(log.messageKey())) {
        continue;
      }
      MDC.remove(field.getKey());
    }
  }
}
