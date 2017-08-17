package io.ctrace;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import static org.cthul.matchers.CthulMatchers.matchesPattern;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.val;
import org.junit.Assert;
import org.junit.Test;


/**
 * Span tests.
 */
public class SpanTest extends BaseTest {

  @Test
  public void testServiceName() {
    Span span = new Span(defaultTracer(),
        "sn",
        null,
        0,
        null,
        null);
    assertEquals("sn", span.service());
  }

  @Test
  public void testOperationName() {
    Span span = new Span(defaultTracer(),
        null,
        "on",
        0,
        null,
        null);
    assertEquals("on", span.operation());
  }

  @Test
  public void testStartMicros() {
    Span span = new Span(defaultTracer(),
        null,
        null,
        123000,
        null,
        null);
    assertEquals(123, span.startMillis());
  }

  @Test
  public void testTags() {
    Map<String, Object> tags = new HashMap<>();
    tags.put("ta", "ta-val");
    tags.put("tb", 345);
    Span span = new Span(defaultTracer(),
        null,
        null,
        0,
        tags,
        null);
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
  public void testTraceId() {
    Span span = new Span(defaultTracer(),
        null,
        null,
        0,
        null,
        null);
    assertThat(span.traceId(), matchesPattern("[0-9a-f]{16}"));
  }

  @Test
  public void testSpanId() {
    Span span = new Span(defaultTracer(),
        null,
        null,
        0,
        null,
        null);
    assertThat(span.spanId(), matchesPattern("[0-9a-f]{16}"));
  }

  @Test
  public void testSetTagString() {
    Span span = new Span(defaultTracer(),
        null,
        null,
        0,
        null,
        null);
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
  public void testSetTagBoolean() {
    Span span = new Span(defaultTracer(),
        null,
        null,
        0,
        null,
        null);
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
  public void testSetTagInt() {
    Span span = new Span(defaultTracer(),
        null,
        null,
        0,
        null,
        null);
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
  public void testLog() {
    Long now = Tools.nowMillis();
    Span span = new Span(defaultTracer(),
        null,
        null,
        0,
        null,
        null);
    Map<String, String> fields = new HashMap<>();
    fields.put("la", "lv");
    span.log(fields);
    assertFalse(this.logger.logs().isEmpty());
    val log = this.logger.logs().get(0);
    assertTrue(log.timestampMillis() >= now);
    assertEquals("la", log.fields()[0].key());
    assertEquals("lv", log.fields()[0].value());
  }

  @Test
  public void testLogEvent() {
    Long now = Tools.nowMillis();
    Span span = new Span(defaultTracer(),
        null,
        null,
        0,
        null,
        null);
    span.log("my-event");
    assertFalse(this.logger.logs().isEmpty());
    val log = this.logger.logs().get(0);
    assertTrue(log.timestampMillis() >= now);
    assertEquals("event", log.fields()[0].key());
    assertEquals("my-event", log.fields()[0].value());
  }

  @Test
  public void testSetGetBaggage() {
    Span span = new Span(defaultTracer(),
        null,
        null,
        0,
        null,
        null);
    span.setBaggageItem("bag1", "val1");
    assertEquals("val1", span.getBaggageItem("bag1"));
  }

  @Test
  public void testParentBaggage() {
    SpanContext parentContext = new SpanContext();
    parentContext.setBaggageItem("pbag1", "pval1");
    parentContext.setBaggageItem("pbag2", "pval2");
    Span span = new Span(defaultTracer(),
        null,
        null,
        0,
        null,
        parentContext);
    assertEquals("pval1", span.getBaggageItem("pbag1"));
    assertEquals("pval2", span.getBaggageItem("pbag2"));
  }

  @Test
  public void testSetOperationName() {
    Span span = new Span(defaultTracer(),
        null,
        "old-op",
        0,
        null,
        null);
    span.setOperationName("new-op");
    assertEquals("new-op", span.operation());
  }

  @Test
  public void testLogEventPayload() {
    Long now = Tools.nowMillis();
    Span span = new Span(defaultTracer(),
        null,
        null,
        0,
        null,
        null);
    //noinspection deprecation
    span.log("my-event", "payload");
    assertTrue(this.logger.logged());
    val log = this.logger.logs().get(0);
    assertTrue(log.timestampMillis() >= now);
    assertEquals("event", log.fields()[0].key());
    assertEquals("my-event", log.fields()[0].value());
    assertEquals("payload", log.fields()[1].key());
    assertEquals("payload", log.fields()[1].value());
  }

  @Test
  public void testFinish() {
    Long now = Tools.nowMillis();
    Span span = new Span(defaultTracer(),
        null,
        null,
        0,
        null,
        null);
    span.finish();
    assertThat(span.finishMillis(), greaterThanOrEqualTo(now));
    assertThat(span.duration(), greaterThanOrEqualTo(Tools.nowMillis() - now));
    assertThat(span.duration(), greaterThan((long) 0));
    val log = this.logger.logs().get(0);
    assertEquals(span.finishMillis(), log.timestampMillis());
    assertEquals("event", log.fields()[0].key());
    assertEquals("Stop-Span", log.fields()[0].value());
  }

  @Test
  public void testContext() {
    Span span = new Span(defaultTracer(),
        null,
        null,
        0,
        null,
        null);
    assertEquals(span.traceId(),
        span.context()
            .traceId());
  }

  @Test
  public void testLogFinishedOnFinish() {
    Span span = new Span(defaultTracer(),
        null,
        "op",
        0,
        null,
        null);
    assertFalse(this.logger.finished());
    span.finish();
    assertTrue(this.logger.finished());
  }

  @Test
  public void testTraceIdKey() {
    Assert.assertEquals("ctrace-trace-id", Span.TRACE_ID);
  }

  @Test
  public void testSpanIdKey() {
    Assert.assertEquals("ctrace-span-id", Span.SPAN_ID);
  }

  @Test
  public void testParentIdKey() {
    Assert.assertEquals("ctrace-parent-id", Span.PARENT_ID);
  }

  @Test
  public void testServiceKey() {
    Assert.assertEquals("ctrace-service", Span.SERVICE);
  }

  @Test
  public void testOperationKey() {
    Assert.assertEquals("ctrace-operation", Span.OPERATION);
  }

  @Test
  public void testStartKey() {
    Assert.assertEquals("ctrace-start", Span.START);
  }

  @Test
  public void testFinishKey() {
    Assert.assertEquals("ctrace-finish", Span.FINISH);
  }

  @Test
  public void testDurationKey() {
    Assert.assertEquals("ctrace-duration", Span.DURATION);
  }

  @Test
  public void testTagsKey() {
    Assert.assertEquals("ctrace-tags", Span.TAGS);
  }

  @Test
  public void testBaggageKey() {
    Assert.assertEquals("ctrace-bag", Span.BAGGAGE);
  }
}
