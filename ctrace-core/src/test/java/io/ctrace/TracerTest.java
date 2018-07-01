package io.ctrace;

import static org.cthul.matchers.CthulMatchers.matchesPattern;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import io.opentracing.References;
import io.opentracing.Scope;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.propagation.TextMapInjectAdapter;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import lombok.val;
import org.junit.Test;

/** Tracer tests. */
public class TracerTest extends BaseTest {

  @Test
  public void testBuilderWithServiceName() {
    Tracer tracer = new Tracer("service-name");
    assertEquals("service-name", tracer.serviceName());
  }

  @Test
  public void testBuilderWithPropagator() {
    Propagator prop = new Propagator(null, null, null, null);
    Tracer tracer = Tracer.builder().propagator(prop).build();
    assertSame(prop, tracer.propagator());
  }

  @Test
  public void testBuilderWithTraceIdExtractHeaders() {
    Tracer tracer = Tracer.builder().traceIdExtractHeader("h1").build();
    assertTrue(tracer.propagator().traceIdExtractHeaders().contains("h1"));
  }

  @Test
  public void testBuilderWithSpanIdExtractHeaders() {
    Tracer tracer = Tracer.builder().spanIdExtractHeader("h1").build();
    assertTrue(tracer.propagator().spanIdExtractHeaders().contains("h1"));
  }

  @Test
  public void testBuilderWithTraceIdInjectHeaders() {
    Tracer tracer = Tracer.builder().traceIdInjectHeader("h1").build();
    assertTrue(tracer.propagator().traceIdInjectHeaders().contains("h1"));
  }

  @Test
  public void testBuilderWithSpanIdInjectHeaders() {
    Tracer tracer = Tracer.builder().spanIdInjectHeader("h1").build();
    assertTrue(tracer.propagator().spanIdInjectHeaders().contains("h1"));
  }

  @Test
  public void testBuilderWithOutputStream() {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    Tracer tracer = Tracer.builder().stream(stream).serviceName("service-name").build();
    new Span(
        tracer, "TestService", "TestOperation", 123000, null, new SpanContext("abc", "def", null));
    String encoded = new String(stream.toByteArray(), StandardCharsets.UTF_8);

    String pattern =
        "\\{\"traceId\":\"abc\",\"spanId\":\"[0-9a-f]{16}\",\"parentId\":\"def\","
            + "\"service\":\"TestService\",\"operation\":\"TestOperation\","
            + "\"start\":123,"
            + "\"log\":\\{\"timestamp\":123,\"event\":\"Start-Span\"\\}\\}\n";

    assertThat(encoded, matchesPattern(pattern));
    assertEquals("service-name", tracer.serviceName());
  }

  @Test
  public void testBuildSpan() {
    Span span = (Span) defaultTracer().buildSpan("my-operation").start();
    assertEquals("my-operation", span.operationName());
  }

  @Test
  public void testBuildSpanAsChild() {
    Span span =
        (Span)
            defaultTracer()
                .buildSpan("my-operation")
                .asChildOf(new SpanContext("abc", "def", null))
                .start();
    assertEquals("abc", span.context().traceId());
    assertEquals("def", span.parentId());
  }

  @Test
  public void testBuildSpanAsChildSpan() {
    Tracer tracer = defaultTracer();
    Span parent = new Span(tracer, null, null, 0, null, null);
    Span span = (Span) tracer.buildSpan("my-operation").asChildOf(parent).start();
    assertEquals(parent.context().traceId(), span.context().traceId());
    assertEquals(parent.context().spanId(), span.parentId());
  }

  @Test
  public void testBuildSpanAddReferenceChildOf() {
    Span span =
        (Span)
            defaultTracer()
                .buildSpan("my-operation")
                .addReference(References.CHILD_OF, new SpanContext("abc", "def", null))
                .start();
    assertEquals("abc", span.context().traceId());
    assertEquals("def", span.parentId());
  }

  @Test
  public void testBuildSpanAddReferenceFollowsFrom() {
    Span span =
        (Span)
            defaultTracer()
                .buildSpan("my-operation")
                .addReference(References.FOLLOWS_FROM, new SpanContext("abc", "def", null))
                .start();
    assertNotEquals("abc", span.context().traceId());
    assertNull(span.parentId());
  }

  @Test
  public void testBuildSpanWithTagString() {
    Span span = (Span) defaultTracer().buildSpan("my-operation").withTag("tk", "tv").start();
    for (Map.Entry<String, ?> tag : span.tags()) {
      assertEquals("tk", tag.getKey());
      assertEquals("tv", tag.getValue());
    }
  }

  @Test
  public void testBuildSpanWithTagBoolean() {
    Span span = (Span) defaultTracer().buildSpan("my-operation").withTag("tk", true).start();
    for (Map.Entry<String, ?> tag : span.tags()) {
      assertEquals("tk", tag.getKey());
      assertEquals(true, tag.getValue());
    }
  }

  @Test
  public void testBuildSpanWithTagNumber() {
    Span span = (Span) defaultTracer().buildSpan("my-operation").withTag("tk", 123).start();
    for (Map.Entry<String, ?> tag : span.tags()) {
      assertEquals("tk", tag.getKey());
      assertEquals(123, tag.getValue());
    }
  }

  @Test
  public void testBuildSpanWithStartTimestamp() {
    Span span = (Span) defaultTracer().buildSpan("my-operation").withStartTimestamp(123000).start();
    assertEquals(123, span.startMillis());
  }

  @Test
  public void testBuildSpanWithStartDeprecated() {
    Span span = (Span) defaultTracer().buildSpan("my-operation").withStartTimestamp(123000).start();
    assertEquals(123, span.startMillis());
  }

  @Test
  public void testInjectHeaders() {
    Map<String, String> headers = new HashMap<>();
    defaultTracer()
        .inject(
            new SpanContext("abc", "def", null),
            Format.Builtin.HTTP_HEADERS,
            new TextMapInjectAdapter(headers));

    assertEquals("abc", headers.get("Ct-Trace-Id"));
    assertEquals("def", headers.get("Ct-Span-Id"));
  }

  @Test
  public void testInjectHeaderBaggage() {
    Map<String, String> headers = new HashMap<>();
    Map<String, String> baggage = new HashMap<>();
    baggage.put("bag1", "val1");
    baggage.put("bag2", "val2");
    defaultTracer()
        .inject(
            new SpanContext("abc", "def", baggage),
            Format.Builtin.HTTP_HEADERS,
            new TextMapInjectAdapter(headers));
    assertEquals("val1", headers.get("Ct-Bag-bag1"));
    assertEquals("val2", headers.get("Ct-Bag-bag2"));
  }

  @Test
  public void testInjectTextMap() {
    Map<String, String> map = new HashMap<>();
    defaultTracer()
        .inject(
            new SpanContext("abc", "def", null),
            Format.Builtin.TEXT_MAP,
            new TextMapInjectAdapter(map));

    assertEquals("abc", map.get("ct-trace-id"));
    assertEquals("def", map.get("ct-span-id"));
  }

  @Test
  public void testInjectTextMapBaggage() {
    Map<String, String> map = new HashMap<>();
    Map<String, String> baggage = new HashMap<>();
    baggage.put("bag1", "val1");
    baggage.put("bag2", "val2");
    defaultTracer()
        .inject(
            new SpanContext("abc", "def", baggage),
            Format.Builtin.TEXT_MAP,
            new TextMapInjectAdapter(map));
    assertEquals("val1", map.get("ct-bag-bag1"));
    assertEquals("val2", map.get("ct-bag-bag2"));
  }

  @Test
  public void testExtractHeaders() {
    Map<String, String> headers = new HashMap<>();
    headers.put("Ct-Trace-Id", "abc");
    headers.put("Ct-Span-Id", "def");
    SpanContext ctx =
        defaultTracer().extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headers));

    assertEquals("abc", ctx.traceId());
    assertEquals("def", ctx.spanId());
  }

  @Test
  public void testExtractHeaderBaggage() {
    Map<String, String> headers = new HashMap<>();
    headers.put("Ct-Trace-Id", "abc");
    headers.put("Ct-Span-Id", "def");
    headers.put("Ct-Bag-Bag1", "val1");
    headers.put("Ct-Bag-Bag2", "val2");
    SpanContext ctx =
        defaultTracer().extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headers));
    assertEquals("val1", ctx.getBaggageItem("Bag1"));
    assertEquals("val2", ctx.getBaggageItem("Bag2"));
  }

  @Test
  public void testExtractTextMap() {
    Map<String, String> map = new HashMap<>();
    map.put("ct-trace-id", "abc");
    map.put("Ct-span-id", "def");
    SpanContext ctx =
        defaultTracer().extract(Format.Builtin.TEXT_MAP, new TextMapExtractAdapter(map));

    assertEquals("abc", ctx.traceId());
    assertEquals("def", ctx.spanId());
  }

  @Test
  public void testExtractTextMapBaggage() {
    Map<String, String> map = new HashMap<>();
    map.put("ct-trace-id", "abc");
    map.put("Ct-span-id", "def");
    map.put("ct-bag-bag1", "val1");
    map.put("Ct-Bag-Bag2", "val2");
    SpanContext ctx =
        defaultTracer().extract(Format.Builtin.TEXT_MAP, new TextMapExtractAdapter(map));
    assertEquals("val1", ctx.getBaggageItem("bag1"));
    assertEquals("val2", ctx.getBaggageItem("Bag2"));
  }

  @Test
  public void testDefaultExtractHeaders1() {
    Map<String, String> headers = new HashMap<>();
    headers.put("X-Correlation-Id", "abc");
    headers.put("X-Request-Id", "def");
    SpanContext ctx =
        defaultTracer().extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headers));
    assertEquals("abc", ctx.traceId());
    assertEquals("def", ctx.spanId());
  }

  @Test
  public void testDefaultExtractHeaders2() {
    Map<String, String> headers = new HashMap<>();
    headers.put("Correlation-Id", "abc");
    headers.put("Request-Id", "def");
    SpanContext ctx =
        defaultTracer().extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headers));
    assertEquals("abc", ctx.traceId());
    assertEquals("def", ctx.spanId());
  }

  @Test
  public void testDefaultExtractHeaders3() {
    Map<String, String> headers = new HashMap<>();
    headers.put("CorrelationId", "abc");
    headers.put("RequestId", "def");
    SpanContext ctx =
        defaultTracer().extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headers));
    assertEquals("abc", ctx.traceId());
    assertEquals("def", ctx.spanId());
  }

  @Test
  public void testDefaultExtractHeaders4() {
    Map<String, String> headers = new HashMap<>();
    headers.put("Correlation_Id", "abc");
    headers.put("Request_Id", "def");
    SpanContext ctx =
        defaultTracer().extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headers));
    assertEquals("abc", ctx.traceId());
    assertEquals("def", ctx.spanId());
  }

  @Test
  public void testDefaultExtractHeaders5() {
    Map<String, String> headers = new HashMap<>();
    headers.put("X_Correlation_Id", "abc");
    headers.put("x_Request_Id", "def");
    SpanContext ctx =
        defaultTracer().extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headers));
    assertEquals("abc", ctx.traceId());
    assertEquals("def", ctx.spanId());
  }

  @Test
  public void testDefaultExtractHeaders6() {
    Map<String, String> headers = new HashMap<>();
    headers.put("X-Trace-Id", "abc");
    headers.put("X-Span-Id", "def");
    SpanContext ctx =
        defaultTracer().extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headers));
    assertEquals("abc", ctx.traceId());
    assertEquals("def", ctx.spanId());
  }

  @Test
  public void testDefaultExtractHeaders7() {
    Map<String, String> headers = new HashMap<>();
    headers.put("X_Trace_Id", "abc");
    headers.put("X_Span_Id", "def");
    SpanContext ctx =
        defaultTracer().extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headers));
    assertEquals("abc", ctx.traceId());
    assertEquals("def", ctx.spanId());
  }

  @Test
  public void testDefaultExtractHeaders8() {
    Map<String, String> headers = new HashMap<>();
    headers.put("Trace-Id", "abc");
    headers.put("Span-Id", "def");
    SpanContext ctx =
        defaultTracer().extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headers));
    assertEquals("abc", ctx.traceId());
    assertEquals("def", ctx.spanId());
  }

  @Test
  public void testDefaultExtractHeaders9() {
    Map<String, String> headers = new HashMap<>();
    headers.put("Trace_Id", "abc");
    headers.put("Span_Id", "def");
    SpanContext ctx =
        defaultTracer().extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headers));
    assertEquals("abc", ctx.traceId());
    assertEquals("def", ctx.spanId());
  }

  @Test
  public void testDefaultExtractHeaders10() {
    Map<String, String> headers = new HashMap<>();
    headers.put("TraceId", "abc");
    headers.put("SpanId", "def");
    SpanContext ctx =
        defaultTracer().extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headers));
    assertEquals("abc", ctx.traceId());
    assertEquals("def", ctx.spanId());
  }

  @Test
  public void testCustomExtractHeaders() {
    Map<String, String> headers = new HashMap<>();
    headers.put("my-trace-id", "abc");
    headers.put("my-span-id", "def");
    SpanContext ctx =
        Tracer.builder()
            .traceIdExtractHeader("my-trace-id")
            .traceIdExtractHeader("your-trace-id")
            .spanIdExtractHeader("your-span-id")
            .spanIdExtractHeader("my-span-id")
            .build()
            .extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headers));
    assertEquals("abc", ctx.traceId());
    assertEquals("def", ctx.spanId());
  }

  @Test
  public void testExtractWithOnlyTraceId() {
    Map<String, String> headers = new HashMap<>();
    headers.put("Ct-Trace-Id", "abc");
    SpanContext ctx =
        defaultTracer().extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headers));
    assertEquals("abc", ctx.traceId());
    assertNull(ctx.spanId());
  }

  @Test
  public void testCustomInjectHeaders() {
    Map<String, String> headers = new HashMap<>();
    Tracer.builder()
        .traceIdInjectHeader("My-trace-id")
        .traceIdInjectHeader("your-trace-id")
        .spanIdInjectHeader("your-Span-id")
        .build()
        .inject(
            new SpanContext("abc", "def", null),
            Format.Builtin.HTTP_HEADERS,
            new TextMapInjectAdapter(headers));
    assertEquals("abc", headers.get("Ct-Trace-Id"));
    assertEquals("abc", headers.get("My-trace-id"));
    assertEquals("abc", headers.get("your-trace-id"));
    assertEquals("def", headers.get("Ct-Span-Id"));
    assertEquals("def", headers.get("your-Span-id"));
  }

  @Test
  public void testStartActive() {
    Tracer tracer = defaultTracer();
    try (Scope scope = tracer.buildSpan("test-op").startActive(true)) {
      val span = scope.span();
      val reactivated = tracer.scopeManager().activate(span, true);
      assertEquals(
          ((SpanContext) span.context()).traceId(),
          ((SpanContext) reactivated.span().context()).traceId());
      assertEquals(
          ((SpanContext) span.context()).spanId(),
          ((SpanContext) reactivated.span().context()).spanId());

      val active = tracer.activeSpan();
      assertEquals(
          ((SpanContext) span.context()).traceId(), ((SpanContext) active.context()).traceId());
      assertEquals(
          ((SpanContext) span.context()).spanId(), ((SpanContext) active.context()).spanId());
    }
  }

  @Test
  public void testStartWithActiveParent() {
    val tracer = defaultTracer();
    val parent = tracer.buildSpan("test-op").startActive(true).span();
    val span = tracer.buildSpan("test-child").start();

    assertEquals(
        ((SpanContext) parent.context()).traceId(), ((SpanContext) span.context()).traceId());
    assertEquals(((SpanContext) parent.context()).spanId(), ((Span) span).parentId());
  }
}
