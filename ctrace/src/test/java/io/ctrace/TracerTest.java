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
import org.junit.jupiter.api.Test;

/** Tracer tests. */
class TracerTest extends BaseTest {

  @Test
  void testBuilderWithServiceName() {
    val tracer = new Tracer("service-name");
    assertEquals("service-name", tracer.serviceName());
  }

  @Test
  void testBuilderWithPropagator() {
    val prop = new Propagator();
    val tracer = defaultTracerBuilder().propagator(prop).build();
    assertSame(prop, tracer.propagator());
  }

  @Test
  void testBuilderWithTraceIdExtractHeaders() {
    val tracer = defaultTracerBuilder().traceIdExtractHeader("h1").build();
    assertTrue(tracer.propagator().traceIdExtractHeaders().contains("h1"));
  }

  @Test
  void testBuilderWithSpanIdExtractHeaders() {
    val tracer = defaultTracerBuilder().spanIdExtractHeader("h1").build();
    assertTrue(tracer.propagator().spanIdExtractHeaders().contains("h1"));
  }

  @Test
  void testBuilderWithTraceIdInjectHeaders() {
    val tracer = defaultTracerBuilder().traceIdInjectHeader("h1").build();
    assertTrue(tracer.propagator().traceIdInjectHeaders().contains("h1"));
  }

  @Test
  void testBuilderWithSpanIdInjectHeaders() {
    val tracer = defaultTracerBuilder().spanIdInjectHeader("h1").build();
    assertTrue(tracer.propagator().spanIdInjectHeaders().contains("h1"));
  }

  @Test
  void testBuilderWithOutputStream() {
    val stream = new ByteArrayOutputStream();
    val tracer = Tracer.builder().stream(stream).serviceName("service-name").build();
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
  void testBuildSpan() {
    Span span = (Span) defaultTracer().buildSpan("my-operation").start();
    assertEquals("my-operation", span.operationName());
  }

  @Test
  void testBuildSpanAsChild() {
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
  void testBuildSpanAsChildSpan() {
    Tracer tracer = defaultTracer();
    Span parent = new Span(tracer, null, null, 0, null, null);
    Span span = (Span) tracer.buildSpan("my-operation").asChildOf(parent).start();
    assertEquals(parent.context().traceId(), span.context().traceId());
    assertEquals(parent.context().spanId(), span.parentId());
  }

  @Test
  void testBuildSpanAddReferenceChildOf() {
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
  void testBuildSpanAddReferenceFollowsFrom() {
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
  void testBuildSpanWithTagString() {
    Span span = (Span) defaultTracer().buildSpan("my-operation").withTag("tk", "tv").start();
    for (Map.Entry<String, ?> tag : span.tags()) {
      assertEquals("tk", tag.getKey());
      assertEquals("tv", tag.getValue());
    }
  }

  @Test
  void testBuildSpanWithTagBoolean() {
    Span span = (Span) defaultTracer().buildSpan("my-operation").withTag("tk", true).start();
    for (Map.Entry<String, ?> tag : span.tags()) {
      assertEquals("tk", tag.getKey());
      assertEquals(true, tag.getValue());
    }
  }

  @Test
  void testBuildSpanWithTagNumber() {
    Span span = (Span) defaultTracer().buildSpan("my-operation").withTag("tk", 123).start();
    for (Map.Entry<String, ?> tag : span.tags()) {
      assertEquals("tk", tag.getKey());
      assertEquals(123, tag.getValue());
    }
  }

  @Test
  void testBuildSpanWithStartTimestamp() {
    Span span = (Span) defaultTracer().buildSpan("my-operation").withStartTimestamp(123000).start();
    assertEquals(123, span.startMillis());
  }

  @Test
  void testBuildSpanWithStartDeprecated() {
    Span span = (Span) defaultTracer().buildSpan("my-operation").withStartTimestamp(123000).start();
    assertEquals(123, span.startMillis());
  }

  @Test
  void testInjectHeaders() {
    Map<String, String> headers = new HashMap<>();
    defaultTracer()
        .inject(
            new SpanContext("abc", "def", null),
            Format.Builtin.HTTP_HEADERS,
            new TextMapInjectAdapter(headers));

    assertEquals("abc", headers.get("X-B3-Traceid"));
    assertEquals("def", headers.get("X-B3-Spanid"));
  }

  @Test
  void testInjectHeaderBaggage() {
    Map<String, String> headers = new HashMap<>();
    Map<String, String> baggage = new HashMap<>();
    baggage.put("bag1", "val1");
    baggage.put("bag2", "val2");
    defaultTracer()
        .inject(
            new SpanContext("abc", "def", baggage),
            Format.Builtin.HTTP_HEADERS,
            new TextMapInjectAdapter(headers));
    assertEquals("val1", headers.get("X-B3-Bag-bag1"));
    assertEquals("val2", headers.get("X-B3-Bag-bag2"));
  }

  @Test
  void testInjectTextMap() {
    Map<String, String> map = new HashMap<>();
    defaultTracer()
        .inject(
            new SpanContext("abc", "def", null),
            Format.Builtin.TEXT_MAP,
            new TextMapInjectAdapter(map));

    assertEquals("abc", map.get("X-B3-Traceid"));
    assertEquals("def", map.get("X-B3-Spanid"));
  }

  @Test
  void testInjectTextMapBaggage() {
    Map<String, String> map = new HashMap<>();
    Map<String, String> baggage = new HashMap<>();
    baggage.put("bag1", "val1");
    baggage.put("bag2", "val2");
    defaultTracer()
        .inject(
            new SpanContext("abc", "def", baggage),
            Format.Builtin.TEXT_MAP,
            new TextMapInjectAdapter(map));
    assertEquals("val1", map.get("X-B3-Bag-bag1"));
    assertEquals("val2", map.get("X-B3-Bag-bag2"));
  }

  @Test
  void testExtractHeaders() {
    Map<String, String> headers = new HashMap<>();
    headers.put("X-B3-Traceid", "abc");
    headers.put("X-B3-Spanid", "def");
    SpanContext ctx =
        defaultTracer().extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headers));

    assertEquals("abc", ctx.traceId());
    assertEquals("def", ctx.spanId());
  }

  @Test
  void testExtractHeaderBaggage() {
    Map<String, String> headers = new HashMap<>();
    headers.put("X-B3-Traceid", "abc");
    headers.put("X-B3-Spanid", "def");
    headers.put("X-B3-Bag-Bag1", "val1");
    headers.put("X-B3-Bag-Bag2", "val2");
    SpanContext ctx =
        defaultTracer().extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headers));
    assertEquals("val1", ctx.getBaggageItem("Bag1"));
    assertEquals("val2", ctx.getBaggageItem("Bag2"));
  }

  @Test
  void testExtractTextMap() {
    Map<String, String> map = new HashMap<>();
    map.put("X-b3-Traceid", "abc");
    map.put("X-b3-spanid", "def");
    SpanContext ctx =
        defaultTracer().extract(Format.Builtin.TEXT_MAP, new TextMapExtractAdapter(map));

    assertEquals("abc", ctx.traceId());
    assertEquals("def", ctx.spanId());
  }

  @Test
  void testExtractTextMapBaggage() {
    Map<String, String> map = new HashMap<>();
    map.put("x-b3-traceid", "abc");
    map.put("x-b3-spanid", "def");
    map.put("x-b3-bag-bag1", "val1");
    map.put("x-b3-Bag-Bag2", "val2");
    SpanContext ctx =
        defaultTracer().extract(Format.Builtin.TEXT_MAP, new TextMapExtractAdapter(map));
    assertEquals("val1", ctx.getBaggageItem("bag1"));
    assertEquals("val2", ctx.getBaggageItem("Bag2"));
  }

  @Test
  void testCustomExtractHeaders() {
    Map<String, String> headers = new HashMap<>();
    headers.put("my-trace-id", "abc");
    headers.put("my-span-id", "def");
    SpanContext ctx =
        defaultTracerBuilder()
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
  void testExtractWithOnlyTraceId() {
    Map<String, String> headers = new HashMap<>();
    headers.put("X-B3-Traceid", "abc");
    SpanContext ctx =
        defaultTracer().extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headers));
    assertEquals("abc", ctx.traceId());
    assertNull(ctx.spanId());
  }

  @Test
  void testCustomInjectHeaders() {
    Map<String, String> headers = new HashMap<>();
    defaultTracerBuilder()
        .traceIdInjectHeader("My-trace-id")
        .traceIdInjectHeader("your-trace-id")
        .spanIdInjectHeader("your-Span-id")
        .build()
        .inject(
            new SpanContext("abc", "def", null),
            Format.Builtin.HTTP_HEADERS,
            new TextMapInjectAdapter(headers));
    assertEquals("abc", headers.get("My-trace-id"));
    assertEquals("abc", headers.get("your-trace-id"));
    assertEquals("def", headers.get("your-Span-id"));
  }

  @Test
  void testStartActive() {
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
  void testStartWithActiveParent() {
    val tracer = defaultTracer();
    val parent = tracer.buildSpan("test-op").startActive(true).span();
    val span = tracer.buildSpan("test-child").start();

    assertEquals(
        ((SpanContext) parent.context()).traceId(), ((SpanContext) span.context()).traceId());
    assertEquals(((SpanContext) parent.context()).spanId(), ((Span) span).parentId());
  }
}
