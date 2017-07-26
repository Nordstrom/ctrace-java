package io.ctrace;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import static org.cthul.matchers.CthulMatchers.matchesPattern;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class SpanTest extends BaseTest {
    @Test
    public void testServiceName() {
        Span span = new Span(defaultTracer(), "sn", null, 0, null, null);
        assertEquals("sn", span.serviceName());
    }

    @Test
    public void testOperationName() {
        Span span = new Span(defaultTracer(), null, "on", 0, null, null);
        assertEquals("on", span.operationName());
    }

    @Test
    public void testStartMicros() {
        Span span = new Span(defaultTracer(), null, null, 123, null, null);
        assertEquals(123, span.startMicros());
    }

    @Test
    public void testTags() {
        Map<String, Object> tags = new HashMap<>();
        tags.put("ta", "ta-val");
        tags.put("tb", 345);
        Span span = new Span(defaultTracer(), null, null, 0, tags, null);
        boolean a = false;
        boolean b = false;
        for (Map.Entry<String, Object> t : span.tagEntries()) {
            if (t.getKey() == "ta" && !a) {
                assertEquals("ta-val", t.getValue());
                a = true;
            } else if (t.getKey() == "tb" && !b){
                assertEquals(345, t.getValue());
                b = true;
            } else {
                fail();
            }
        }
    }

    @Test
    public void testTraceId() {
        Span span = new Span(defaultTracer(), null, null, 0, null, null);
        assertThat(span.traceId(), matchesPattern("[0-9a-f]{32}"));
    }

    @Test
    public void testSpanId() {
        Span span = new Span(defaultTracer(), null, null, 0, null, null);
        assertThat(span.spanId(), matchesPattern("[0-9a-f]{32}"));
    }

    @Test
    public void testConstructorLog() {
        Span span = new Span(defaultTracer(), null, null, 123, null, null);
        assertEquals(123, span.log().timestampMicros());
        for (Map.Entry<String, ?> f : span.log().fields()) {
            assertEquals("event", f.getKey());
            assertEquals("Start-Span", f.getValue());
            return;
        }
    }

    @Test
    public void testConstructorLogWithSingleEvent() {
        Span span = new Span(singleEventTracer(), null, null, 123, null, null);
        ArrayList<LogEntry> logs = (ArrayList<LogEntry>)span.logEntries();
        assertEquals(1, logs.size());
        LogEntry log = logs.get(0);
        assertEquals(123, log.timestampMicros());
        for (Map.Entry<String, ?> f : log.fields()) {
            assertEquals("event", f.getKey());
            assertEquals("Start-Span", f.getValue());
            return;
        }
    }

    @Test
    public void testSetTagString() {
        Span span = new Span(defaultTracer(), null, null, 0, null, null);
        span.setTag("ta", "ta-val");
        boolean a = false;
        for (Map.Entry<String, Object> t : span.tagEntries()) {
            if (t.getKey() == "ta" && !a) {
                assertEquals("ta-val", t.getValue());
                a = true;
            } else {
                fail();
            }
        }
    }

    @Test
    public void testSetTagBoolean() {
        Span span = new Span(defaultTracer(), null, null, 0, null, null);
        span.setTag("ta", true);
        boolean a = false;
        for (Map.Entry<String, Object> t : span.tagEntries()) {
            if (t.getKey() == "ta" && !a) {
                assertEquals(true, t.getValue());
                a = true;
            } else {
                fail();
            }
        }
    }

    @Test
    public void testSetTagInt() {
        Span span = new Span(defaultTracer(), null, null, 0, null, null);
        span.setTag("ta", 345);
        boolean a = false;
        for (Map.Entry<String, Object> t : span.tagEntries()) {
            if (t.getKey() == "ta" && !a) {
                assertEquals(345, t.getValue());
                a = true;
            } else {
                fail();
            }
        }
    }

    @Test
    public void testLog() {
        Long now = Tools.nowMicros();
        Span span = new Span(defaultTracer(), null, null, 0, null, null);
        Map<String, String> fields = new HashMap<>();
        fields.put("la", "lv");
        span.log(fields);
        assertTrue(span.log().timestampMicros() >= now);
        for (Map.Entry<String, ?> f : span.log().fields()) {
            assertEquals("la", f.getKey());
            assertEquals("lv", f.getValue());
            return;
        }
    }

    @Test
    public void testLogEvent() {
        Long now = Tools.nowMicros();
        Span span = new Span(defaultTracer(), null, null, 0, null, null);
        span.log("my-event");
        assertTrue(span.log().timestampMicros() >= now);
        for (Map.Entry<String, ?> f : span.log().fields()) {
            assertEquals("event", f.getKey());
            assertEquals("my-event", f.getValue());
            return;
        }
    }

    @Test
    public void testSetGetBaggage() {
        Span span = new Span(defaultTracer(), null, null, 0, null, null);
        span.setBaggageItem("bag1", "val1");
        assertEquals("val1", span.getBaggageItem("bag1"));
    }

    @Test
    public void testParentBaggage() {
        SpanContext parentContext = new SpanContext();
        parentContext.setBaggageItem("pbag1", "pval1");
        parentContext.setBaggageItem("pbag2", "pval2");
        Span span = new Span(defaultTracer(), null, null, 0, null, parentContext);
        assertEquals("pval1", span.getBaggageItem("pbag1"));
        assertEquals("pval2", span.getBaggageItem("pbag2"));
    }

    @Test
    public void testSetOperationName() {
        Span span = new Span(defaultTracer(), null, "old-op", 0, null, null);
        span.setOperationName("new-op");
        assertEquals("new-op", span.operationName());
    }

    @Test
    public void testLogEventPayload() {
        Long now = Tools.nowMicros();
        Span span = new Span(defaultTracer(), null, null, 0, null, null);
        span.log("my-event", "payload");
        assertTrue(span.log().timestampMicros() >= now);
        boolean e =false, p = false;
        for (Map.Entry<String, ?> f : span.log().fields()) {
            if (f.getKey() == "event" && !e) {
                assertEquals("my-event", f.getValue());
                e = true;
            } else if (f.getKey() == "payload" && !p) {
                assertEquals("payload", f.getValue());
                p = true;
            } else {
                fail();
            }
        }
    }

    @Test
    public void testFinish() {
        Long now = Tools.nowMicros();
        Span span = new Span(defaultTracer(), null, null, 0, null, null);
        span.finish();
        assertThat(span.finishMicros(), greaterThanOrEqualTo(now));
        assertThat(span.duration(), greaterThanOrEqualTo(Tools.nowMicros() - now));
        assertThat(span.duration(), greaterThan((long)0));
        assertEquals(span.finishMicros(), span.log().timestampMicros());
        for (Map.Entry<String, ?> f : span.log().fields()) {
            assertEquals("event", f.getKey());
            assertEquals("Stop-Span", f.getValue());
            return;
        }
    }

    @Test
    public void testContext() {
        Span span = new Span(defaultTracer(), null, null, 0, null, null);
        assertEquals(span.traceId(), span.context().traceId());
    }
}
