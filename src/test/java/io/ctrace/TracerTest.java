package io.ctrace;

import io.opentracing.ActiveSpan;
import io.opentracing.References;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.propagation.TextMapInjectAdapter;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.cthul.matchers.CthulMatchers.matchesPattern;
import static org.junit.Assert.*;

public class TracerTest extends BaseTest {
    @Test
    public void testConstructor() {
        Tracer tracer = new Tracer();
        assertEquals(false, tracer.singleSpanOutput());
        assertEquals(null, tracer.serviceName());
    }

    @Test
    public void testBuilderWithServiceName() {
        Tracer tracer = Tracer.withServiceName("service-name")
                              .build();
        assertEquals(false, tracer.singleSpanOutput());
        assertEquals("service-name", tracer.serviceName());
    }

    @Test
    public void testBuilderWithPropagator() {
        Propagator prop = new Propagator(null, null, null, null);
        Tracer tracer = Tracer.withPropagator(prop)
                              .build();
        assertSame(prop, tracer.propagator());
    }

    @Test
    public void testBuilderWithTraceIdExtractHeaders() {
        Tracer tracer = Tracer.withTraceIdExtractHeaders("h1").build();
        assertTrue(tracer.propagator().traceIdExtractHeaders.contains("h1"));
    }

    @Test
    public void testBuilderWithSpanIdExtractHeaders() {
        Tracer tracer = Tracer.withSpanIdExtractHeaders("h1").build();
        assertTrue(tracer.propagator().spanIdExtractHeaders.contains("h1"));
    }

    @Test
    public void testBuilderWithTraceIdInjectHeaders() {
        Tracer tracer = Tracer.withTraceIdInjectHeaders("h1").build();
        assertArrayEquals(new String[]{"h1"}, tracer.propagator().traceIdInjectHeaders);
    }

    @Test
    public void testBuilderWithSpanIdInjectHeaders() {
        Tracer tracer = Tracer.withSpanIdInjectHeaders("h1").build();
        assertArrayEquals(new String[]{"h1"}, tracer.propagator().spanIdInjectHeaders);
    }

    @Test
    public void testBuilderWithSingleSpanOutput() {
        Tracer tracer = Tracer.withSingleSpanOutput(true)
                              .build();
        assertEquals(true, tracer.singleSpanOutput());
        assertEquals(null, tracer.serviceName());
    }

    @Test
    public void testBuilderWithOutputStream() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Tracer tracer = Tracer.withStream(stream)
                              .build();
        new Span(tracer,
                 "TestService",
                 "TestOperation",
                 123,
                 null,
                 new SpanContext("abc", "def", null));
        String encoded = new String(stream.toByteArray(), StandardCharsets.UTF_8);

        String pattern =
                "\\{\"traceId\":\"abc\",\"spanId\":\"[0-9a-f]{32}\",\"parentId\":\"def\"," +
                        "\"service\":\"TestService\",\"operation\":\"TestOperation\",\"start\":123," +
                        "\"log\":\\{\"timestamp\":123,\"event\":\"Start-Span\"\\}\\}\n";

        assertThat(encoded, matchesPattern(pattern));
        assertEquals(false, tracer.singleSpanOutput());
        assertEquals(null, tracer.serviceName());
    }

    @Test
    public void testBuilderWithOutputStreamAndServiceName() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Tracer tracer = Tracer.withStream(stream)
                              .withServiceName("service-name")
                              .build();
        new Span(tracer,
                 "TestService",
                 "TestOperation",
                 123,
                 null,
                 new SpanContext("abc", "def", null));
        String encoded = new String(stream.toByteArray(), StandardCharsets.UTF_8);

        String pattern =
                "\\{\"traceId\":\"abc\",\"spanId\":\"[0-9a-f]{32}\",\"parentId\":\"def\"," +
                        "\"service\":\"TestService\",\"operation\":\"TestOperation\",\"start\":123," +
                        "\"log\":\\{\"timestamp\":123,\"event\":\"Start-Span\"\\}\\}\n";

        assertThat(encoded, matchesPattern(pattern));
        assertEquals(false, tracer.singleSpanOutput());
        assertEquals("service-name", tracer.serviceName());
    }

    @Test
    public void testBuilderWithOutputStreamAndSingleSpanOutput() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Tracer tracer = Tracer.withStream(stream)
                              .withSingleSpanOutput(true)
                              .build();
        Span span = new Span(tracer,
                             "TestService",
                             "TestOperation",
                             123,
                             null,
                             new SpanContext("abc", "def", null));
        tracer.report(span);
        String encoded = new String(stream.toByteArray(), StandardCharsets.UTF_8);

        String pattern =
                "\\{\"traceId\":\"abc\",\"spanId\":\"[0-9a-f]{32}\",\"parentId\":\"def\"," +
                        "\"service\":\"TestService\",\"operation\":\"TestOperation\",\"start\":123," +
                        "\"logs\":\\[\\{\"timestamp\":123,\"event\":\"Start-Span\"\\}\\]\\}\n";

        assertThat(encoded, matchesPattern(pattern));
        assertEquals(true, tracer.singleSpanOutput());
        assertEquals(null, tracer.serviceName());
    }

    @Test
    public void testBuildSpan() {
        Span span = defaultTracer().buildSpan("my-operation")
                                   .startManual();
        assertEquals("my-operation", span.operationName());
    }

    @Test
    public void testBuildSpanAsChild() {
        Span span = defaultTracer()
                .buildSpan("my-operation")
                .asChildOf(new SpanContext("abc", "def", null))
                .startManual();
        assertEquals("abc", span.traceId());
        assertEquals("def", span.parentId());
    }

    @Test
    public void testBuildSpanAsChildSpan() {
        Tracer tracer = defaultTracer();
        Span parent = new Span(tracer, null, null, 0, null, null);
        Span span = tracer
                .buildSpan("my-operation")
                .asChildOf(parent)
                .startManual();
        assertEquals(parent.traceId(), span.traceId());
        assertEquals(parent.spanId(), span.parentId());
    }

    @Test
    public void testBuildSpanAsReferenceSpan() {
        Span span = defaultTracer()
                .buildSpan("my-operation")
                .addReference(References.CHILD_OF, new SpanContext("abc", "def", null))
                .startManual();
        assertEquals("abc", span.traceId());
        assertEquals("def", span.parentId());
    }

    @Test
    public void testBuildSpanWithTagString() {
        Span span = defaultTracer()
                .buildSpan("my-operation")
                .withTag("tk", "tv")
                .startManual();
        for (Map.Entry<String, ?> tag : span.tagEntries()) {
            assertEquals("tk", tag.getKey());
            assertEquals("tv", tag.getValue());
        }
    }

    @Test
    public void testBuildSpanWithTagBoolean() {
        Span span = defaultTracer()
                .buildSpan("my-operation")
                .withTag("tk", true)
                .startManual();
        for (Map.Entry<String, ?> tag : span.tagEntries()) {
            assertEquals("tk", tag.getKey());
            assertEquals(true, tag.getValue());
        }
    }

    @Test
    public void testBuildSpanWithTagNumber() {
        Span span = defaultTracer()
                .buildSpan("my-operation")
                .withTag("tk", 123)
                .startManual();
        for (Map.Entry<String, ?> tag : span.tagEntries()) {
            assertEquals("tk", tag.getKey());
            assertEquals(123, tag.getValue());
        }
    }

    @Test
    public void testBuildSpanWithStartTimestamp() {
        Span span = defaultTracer()
                .buildSpan("my-operation")
                .withStartTimestamp(123)
                .startManual();
        assertEquals(123, span.startMicros());
    }

    @Test
    public void testBuildSpanWithStartDeprecated() {
        Span span = defaultTracer()
                .buildSpan("my-operation")
                .withStartTimestamp(123)
                .start();
        assertEquals(123, span.startMicros());
    }

    @Test
    public void testInjectHeaders() {
        Map<String, String> headers = new HashMap<>();
        defaultTracer().inject(
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
        defaultTracer().inject(
                new SpanContext("abc", "def", baggage),
                Format.Builtin.HTTP_HEADERS,
                new TextMapInjectAdapter(headers));
        assertEquals("val1", headers.get("Ct-Bag-bag1"));
        assertEquals("val2", headers.get("Ct-Bag-bag2"));
    }

    @Test
    public void testInjectTextMap() {
        Map<String, String> map = new HashMap<>();
        defaultTracer().inject(
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
        defaultTracer().inject(
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
        SpanContext ctx = defaultTracer().extract(Format.Builtin.HTTP_HEADERS,
                                                  new TextMapExtractAdapter(headers));

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
        SpanContext ctx = defaultTracer().extract(Format.Builtin.HTTP_HEADERS,
                                                  new TextMapExtractAdapter(headers));
        assertEquals("val1", ctx.getBaggageItem("Bag1"));
        assertEquals("val2", ctx.getBaggageItem("Bag2"));
    }

    @Test
    public void testExtractTextMap() {
        Map<String, String> map = new HashMap<>();
        map.put("ct-trace-id", "abc");
        map.put("Ct-span-id", "def");
        SpanContext ctx = defaultTracer().extract(Format.Builtin.TEXT_MAP,
                                                  new TextMapExtractAdapter(map));

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
        SpanContext ctx = defaultTracer().extract(Format.Builtin.TEXT_MAP,
                                                  new TextMapExtractAdapter(map));
        assertEquals("val1", ctx.getBaggageItem("bag1"));
        assertEquals("val2", ctx.getBaggageItem("Bag2"));
    }

    @Test
    public void testDefaultExtractHeaders1() {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Correlation-Id", "abc");
        headers.put("X-Request-Id", "def");
        SpanContext ctx = defaultTracer().extract(Format.Builtin.HTTP_HEADERS,
                                                  new TextMapExtractAdapter(headers));
        assertEquals("abc", ctx.traceId());
        assertEquals("def", ctx.spanId());
    }

    @Test
    public void testDefaultExtractHeaders2() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Correlation-Id", "abc");
        headers.put("Request-Id", "def");
        SpanContext ctx = defaultTracer().extract(Format.Builtin.HTTP_HEADERS,
                                                  new TextMapExtractAdapter(headers));
        assertEquals("abc", ctx.traceId());
        assertEquals("def", ctx.spanId());
    }

    @Test
    public void testDefaultExtractHeaders3() {
        Map<String, String> headers = new HashMap<>();
        headers.put("CorrelationId", "abc");
        headers.put("RequestId", "def");
        SpanContext ctx = defaultTracer().extract(Format.Builtin.HTTP_HEADERS,
                                                  new TextMapExtractAdapter(headers));
        assertEquals("abc", ctx.traceId());
        assertEquals("def", ctx.spanId());
    }

    @Test
    public void testDefaultExtractHeaders4() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Correlation_Id", "abc");
        headers.put("Request_Id", "def");
        SpanContext ctx = defaultTracer().extract(Format.Builtin.HTTP_HEADERS,
                                                  new TextMapExtractAdapter(headers));
        assertEquals("abc", ctx.traceId());
        assertEquals("def", ctx.spanId());
    }

    @Test
    public void testDefaultExtractHeaders5() {
        Map<String, String> headers = new HashMap<>();
        headers.put("X_Correlation_Id", "abc");
        headers.put("x_Request_Id", "def");
        SpanContext ctx = defaultTracer().extract(Format.Builtin.HTTP_HEADERS,
                                                  new TextMapExtractAdapter(headers));
        assertEquals("abc", ctx.traceId());
        assertEquals("def", ctx.spanId());
    }

    @Test
    public void testDefaultExtractHeaders6() {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Trace-Id", "abc");
        headers.put("X-Span-Id", "def");
        SpanContext ctx = defaultTracer().extract(Format.Builtin.HTTP_HEADERS,
                                                  new TextMapExtractAdapter(headers));
        assertEquals("abc", ctx.traceId());
        assertEquals("def", ctx.spanId());
    }

    @Test
    public void testDefaultExtractHeaders7() {
        Map<String, String> headers = new HashMap<>();
        headers.put("X_Trace_Id", "abc");
        headers.put("X_Span_Id", "def");
        SpanContext ctx = defaultTracer().extract(Format.Builtin.HTTP_HEADERS,
                                                  new TextMapExtractAdapter(headers));
        assertEquals("abc", ctx.traceId());
        assertEquals("def", ctx.spanId());
    }

    @Test
    public void testDefaultExtractHeaders8() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Trace-Id", "abc");
        headers.put("Span-Id", "def");
        SpanContext ctx = defaultTracer().extract(Format.Builtin.HTTP_HEADERS,
                                                  new TextMapExtractAdapter(headers));
        assertEquals("abc", ctx.traceId());
        assertEquals("def", ctx.spanId());
    }

    @Test
    public void testDefaultExtractHeaders9() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Trace_Id", "abc");
        headers.put("Span_Id", "def");
        SpanContext ctx = defaultTracer().extract(Format.Builtin.HTTP_HEADERS,
                                                  new TextMapExtractAdapter(headers));
        assertEquals("abc", ctx.traceId());
        assertEquals("def", ctx.spanId());
    }

    @Test
    public void testDefaultExtractHeaders10() {
        Map<String, String> headers = new HashMap<>();
        headers.put("TraceId", "abc");
        headers.put("SpanId", "def");
        SpanContext ctx = defaultTracer().extract(Format.Builtin.HTTP_HEADERS,
                                                  new TextMapExtractAdapter(headers));
        assertEquals("abc", ctx.traceId());
        assertEquals("def", ctx.spanId());
    }

    @Test
    public void testCustomExtractHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("my-trace-id", "abc");
        headers.put("my-span-id", "def");
        SpanContext ctx = Tracer
                .withTraceIdExtractHeaders("my-trace-id", "your-trace-id")
                .withSpanIdExtractHeaders("your-span-id", "my-span-id")
                .build()
                .extract(Format.Builtin.HTTP_HEADERS,
                         new TextMapExtractAdapter(headers));
        assertEquals("abc", ctx.traceId());
        assertEquals("def", ctx.spanId());
    }

    @Test
    public void testExtractWithOnlyTraceId() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Ct-Trace-Id", "abc");
        SpanContext ctx = defaultTracer()
                .extract(Format.Builtin.HTTP_HEADERS,
                         new TextMapExtractAdapter(headers));
        assertEquals("abc", ctx.traceId());
        assertNull(ctx.spanId());
    }

    @Test
    public void testCustomInjectHeaders() {
        Map<String, String> headers = new HashMap<>();
        Tracer
                .withTraceIdInjectHeaders("My-trace-id", "your-trace-id")
                .withSpanIdInjectHeaders("your-Span-id")
                .build()
                .inject(new SpanContext("abc", "def", null),
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
        ActiveSpan span = tracer
                .buildSpan("test-op")
                .startActive();
        ActiveSpan.Continuation cont = span.capture();
        ActiveSpan reactivated = cont.activate();
        assertEquals(((SpanContext)span.context()).traceId(), ((SpanContext)reactivated.context()).traceId());
        assertEquals(((SpanContext)span.context()).spanId(), ((SpanContext)reactivated.context()).spanId());

        ActiveSpan active = tracer.activeSpan();
        assertEquals(((SpanContext)span.context()).traceId(), ((SpanContext)active.context()).traceId());
        assertEquals(((SpanContext)span.context()).spanId(), ((SpanContext)active.context()).spanId());
    }
}
