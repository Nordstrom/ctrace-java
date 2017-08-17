package io.ctrace;


import static org.cthul.matchers.CthulMatchers.matchesPattern;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

/**
 * StreamLogger tests.
 */
public class StreamLoggerTest extends BaseTest {

  @Test
  public void testStart() {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    Logger logger = new StreamLogger(stream, new JsonEncoder());
    Span span = new Span(defaultTracer(),
        "TestService",
        "TestOperation",
        123000,
        null,
        new SpanContext("abc", "def", null));
    logger.start(span, new Log(123, "Start-Span"));
    String encoded = new String(stream.toByteArray(), StandardCharsets.UTF_8);

    String pattern =
        "\\{\"traceId\":\"abc\",\"spanId\":\"[0-9a-f]{16}\",\"parentId\":\"def\","
            + "\"service\":\"TestService\",\"operation\":\"TestOperation\","
            + "\"start\":123,"
            + "\"log\":\\{\"timestamp\":123,\"event\":\"Start-Span\"\\}\\}\n";

    assertThat(encoded, matchesPattern(pattern));
  }
}
