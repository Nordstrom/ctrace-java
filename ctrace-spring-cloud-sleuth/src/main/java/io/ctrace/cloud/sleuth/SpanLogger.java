package io.ctrace.cloud.sleuth;

import io.ctrace.JsonEncoder;
import io.ctrace.Span;
import io.ctrace.Tools;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;

public class SpanLogger implements org.springframework.cloud.sleuth.log.SpanLogger {
  private final Logger logger = org.slf4j.LoggerFactory.getLogger(SpanLogger.class);

  @Value("${spring.application.name}")
  private String service;

  /**
   * Logic to run when a Span gets started.
   *
   * @param parent - maybe be nullable
   * @param span - current span
   */
  @Override
  public void logStartedSpan(
      org.springframework.cloud.sleuth.Span parent,
      org.springframework.cloud.sleuth.Span span) {
    setMdc(parent, span);
    logger.info("Start-Span");
  }

  /**
   * Logic to run when a Span gets continued.
   */
  @Override
  public void logContinuedSpan(org.springframework.cloud.sleuth.Span span) {
    setMdc(null, span);
  }

  /**
   * Logic to run when a Span gets stopped (closed or detached).
   *
   * @param parent - maybe be nullable
   * @param span - current span
   */
  @Override
  public void logStoppedSpan(
      org.springframework.cloud.sleuth.Span parent, 
      org.springframework.cloud.sleuth.Span span) {
    logger.info("Stop-Span");
    MDC.remove(Span.PARENT_ID);
    if (parent != null && span != null) {
      setMdc(null, parent);
    } else {
      MDC.remove(Span.TRACE_ID);
      MDC.remove(Span.SPAN_ID);
      MDC.remove(Span.SERVICE);
      MDC.remove(Span.OPERATION);
      MDC.remove(Span.START);
      MDC.remove(Span.FINISH);
      MDC.remove(Span.DURATION);
      MDC.remove(Span.TAGS);
      MDC.remove(Span.BAGGAGE);
    }
  }

  private void setMdc(
      org.springframework.cloud.sleuth.Span parent,
      org.springframework.cloud.sleuth.Span span) {
    val begin = span.getBegin();
    val end = span.getEnd();
    val tags = span.tags();
    val baggage = span.getBaggage();

    MDC.put(Span.TRACE_ID, Tools.longToHex(span.getTraceId()));
    MDC.put(Span.SPAN_ID, Tools.longToHex(span.getSpanId()));
    if (parent != null) {
      MDC.put(Span.PARENT_ID, Tools.longToHex(parent.getSpanId()));
    } else {
      val parents = span.getParents();
      if (parents != null && !parents.isEmpty()) {
        MDC.put(Span.PARENT_ID, Tools.longToHex(parents.get(0)));
      }
    }

    if (this.service == null) {
      this.service = System.getenv("ctrace_service_name");
    }
    MDC.put(Span.SERVICE, this.service);
    MDC.put(Span.OPERATION, span.getName());
    MDC.put(Span.START, String.valueOf(begin));
    if (end > 0) {
      MDC.put(Span.FINISH, String.valueOf(end));
      MDC.put(Span.DURATION, String.valueOf(end - begin));
    }

//    if (tags != null && !tags.isEmpty()) {
//      MDC.put(Span.TAGS, JsonEncoder.encodeTags(tags.entrySet()));
//    }
//    if (baggage != null && !baggage.isEmpty()) {
//      MDC.put(Span.BAGGAGE, JsonEncoder.encodeBaggage(baggage.entrySet()));
//    }

  }
}
