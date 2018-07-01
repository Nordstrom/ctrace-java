package io.ctrace;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import static org.cthul.matchers.CthulMatchers.matchesPattern;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.hamcrest.number.OrderingComparison.lessThanOrEqualTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.val;
import org.junit.jupiter.api.Test;

/** Span tests. */
class SpanTest extends BaseTest {

  @Test
  void testServiceName() {
    Span span = new Span(defaultTracer(), "sn", null, 0, null, null);
    assertEquals("sn", span.serviceName());
  }

  @Test
  void testOperationName() {
    Span span = new Span(defaultTracer(), null, "on", 0, null, null);
    assertEquals("on", span.operationName());
  }

  @Test
  void testStartMicros() {
    Span span = new Span(defaultTracer(), null, null, 123000, null, null);
    assertEquals(123, span.startMillis());
  }

  @Test
  void testTags() {
    Map<String, Object> tags = new HashMap<>();
    tags.put("ta", "ta-val");
    tags.put("tb", 345);
    Span span = new Span(defaultTracer(), null, null, 0, tags, null);
    boolean a = false;
    boolean b = false;
    for (Map.Entry<String, ?> t : span.tags()) {
      if (Objects.equals(t.getKey(), "ta") && !a) {
        assertEquals("ta-val", t.getValue());
        a = true;
      } else if (Objects.equals(t.getKey(), "tb") && !b) {
        assertEquals(345, t.getValue());
        b = true;
      } else {
        fail();
      }
    }
  }

  @Test
  void testTraceId() {
    Span span = new Span(defaultTracer(), null, null, 0, null, null);
    assertThat(span.context().traceId(), matchesPattern("[0-9a-f]{16}"));
  }

  @Test
  void testSpanId() {
    Span span = new Span(defaultTracer(), null, null, 0, null, null);
    assertThat(span.context().spanId(), matchesPattern("[0-9a-f]{16}"));
  }

  @Test
  void testSetTagString() {
    Span span = new Span(defaultTracer(), null, null, 0, null, null);
    span.setTag("ta", "ta-val");
    boolean a = false;
    for (Map.Entry<String, ?> t : span.tags()) {
      if (Objects.equals(t.getKey(), "ta") && !a) {
        assertEquals("ta-val", t.getValue());
        a = true;
      } else {
        fail();
      }
    }
  }

  @Test
  void testSetTagBoolean() {
    Span span = new Span(defaultTracer(), null, null, 0, null, null);
    span.setTag("ta", true);
    boolean a = false;
    for (Map.Entry<String, ?> t : span.tags()) {
      if (Objects.equals(t.getKey(), "ta") && !a) {
        assertEquals(true, t.getValue());
        a = true;
      } else {
        fail();
      }
    }
  }

  @Test
  void testSetTagInt() {
    Span span = new Span(defaultTracer(), null, null, 0, null, null);
    span.setTag("ta", 345);
    boolean a = false;
    for (Map.Entry<String, ?> t : span.tags()) {
      if (Objects.equals(t.getKey(), "ta") && !a) {
        assertEquals(345, t.getValue());
        a = true;
      } else {
        fail();
      }
    }
  }

  @Test
  void testLog() {
    Long now = Tools.nowMillis();
    Span span = new Span(defaultTracer(), null, null, 0, null, null);
    Map<String, String> fields = new HashMap<>();
    fields.put("la", "lv");
    span.log(fields);
    assertFalse(this.logger.logs().isEmpty());
    val log = this.logger.logs().get(1);
    assertTrue(log.timestampMillis() >= now);
    assertEquals("lv", log.fields().get("la"));
  }

  @Test
  void testLogEvent() {
    Long now = Tools.nowMillis();
    Span span = new Span(defaultTracer(), null, null, 0, null, null);
    span.log("my-event");
    assertFalse(this.logger.logs().isEmpty());
    val log = this.logger.logs().get(1);
    assertTrue(log.timestampMillis() >= now);
    assertEquals("my-event", log.fields().get("event"));
  }

  @Test
  void testSetGetBaggage() {
    Span span = new Span(defaultTracer(), null, null, 0, null, null);
    span.setBaggageItem("bag1", "val1");
    assertEquals("val1", span.getBaggageItem("bag1"));
  }

  @Test
  void testParentBaggage() {
    SpanContext parentContext = new SpanContext();
    parentContext.setBaggageItem("pbag1", "pval1");
    parentContext.setBaggageItem("pbag2", "pval2");
    Span span = new Span(defaultTracer(), null, null, 0, null, parentContext);
    assertEquals("pval1", span.getBaggageItem("pbag1"));
    assertEquals("pval2", span.getBaggageItem("pbag2"));
  }

  @Test
  void testSetOperationName() {
    Span span = new Span(defaultTracer(), null, "old-op", 0, null, null);
    span.setOperationName("new-op");
    assertEquals("new-op", span.operationName());
  }

  @Test
  void testFinish() {
    Long now = Tools.nowMillis();
    Span span = new Span(defaultTracer(), null, null, 0, null, null);
    span.finish();
    assertThat(span.finishMillis(), greaterThanOrEqualTo(now));
    assertThat(span.duration(), lessThanOrEqualTo(Tools.nowMillis() - now));
    assertThat(span.duration(), greaterThanOrEqualTo((long) 0));
    val log = this.logger.logs().get(1);
    assertEquals(span.finishMillis(), log.timestampMillis());
    assertEquals("Stop-Span", log.fields().get("event"));
  }

  @Test
  void testContext() {
    Span span = new Span(defaultTracer(), null, null, 0, null, null);
    assertEquals(span.context().traceId(), span.context().traceId());
  }

  @Test
  void testLogFinishedOnFinish() {
    Span span = new Span(defaultTracer(), null, "op", 0, null, null);
    assertFalse(this.logger.finished());
    span.finish();
    assertTrue(this.logger.finished());
  }
}
