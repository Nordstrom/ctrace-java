package io.ctrace;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.cthul.matchers.CthulMatchers.matchesPattern;
import static org.junit.Assert.assertThat;

public class StreamReporterTest extends BaseTest {
    @Test
    public void testStreamReporter() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Reporter reporter = new StreamReporter(stream, new JsonEncoder());
        Span span = new Span(defaultTracer(),
                             "TestService",
                             "TestOperation",
                             123,
                             null,
                             new SpanContext("abc", "def", null));
        reporter.report(span);
        String encoded = new String(stream.toByteArray(), StandardCharsets.UTF_8);

        String pattern =
                "\\{\"traceId\":\"abc\",\"spanId\":\"[0-9a-f]{32}\",\"parentId\":\"def\"," +
                        "\"service\":\"TestService\",\"operation\":\"TestOperation\",\"start\":123," +
                        "\"log\":\\{\"timestamp\":123,\"event\":\"Start-Span\"\\}\\}\n";

        assertThat(encoded, matchesPattern(pattern));
    }
}
