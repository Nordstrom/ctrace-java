package io.ctrace;

import static org.cthul.matchers.CthulMatchers.matchesPattern;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/**
 * JsonEncoder tests.
 */
public class JsonEncoderTest extends BaseTest {

  @Test
  public void testNewSpan() {
    String encoded = new String(
        new JsonEncoder()
            .encodeToBytes(new Span(defaultTracer(),
                "TestService",
                "TestOperation",
                123000,
                null,
                new SpanContext("abc",
                    "def",
                    null)).reportable()));

    String pattern =
        "\\{\"traceId\":\"abc\",\"spanId\":\"[0-9a-f]{16}\",\"parentId\":\"def\","
            + "\"service\":\"TestService\",\"operation\":\"TestOperation\","
            + "\"start\":123,"
            + "\"log\":\\{\"timestamp\":123,\"event\":\"Start-Span\"\\}\\}\n";

    assertThat(encoded, matchesPattern(pattern));
  }

  @Test
  public void testNewSpanWithTags() {
    Map<String, Object> tags = new HashMap<>();
    tags.put("t1", "tval1");
    tags.put("t2", 55);
    tags.put("t3", true);
    String encoded = new String(
        new JsonEncoder()
            .encodeToBytes(new Span(defaultTracer(),
                "TestService",
                "TestOperation",
                123000,
                tags,
                new SpanContext("abc",
                    "def",
                    null)).reportable()));

    String pattern =
        "\\{\"traceId\":\"abc\",\"spanId\":\"[0-9a-f]{16}\",\"parentId\":\"def\","
            + "\"service\":\"TestService\",\"operation\":\"TestOperation\","
            + "\"start\":123,"
            + "\"tags\":\\{\"t1\":\"tval1\",\"t2\":55,\"t3\":true\\},"
            + "\"log\":\\{\"timestamp\":123,\"event\":\"Start-Span\"\\}\\}\n";

    assertThat(encoded, matchesPattern(pattern));
  }

  @Test
  public void testNewSpanWithBaggage() {
    Map<String, String> bag = new HashMap<>();
    bag.put("a", "av");
    bag.put("b", "bv");
    String encoded = new String(
        new JsonEncoder()
            .encodeToBytes(new Span(defaultTracer(),
                "TestService",
                "TestOperation",
                123000,
                null,
                new SpanContext("abc", "def", bag))
                .reportable()));

    String pattern =
        "\\{\"traceId\":\"abc\",\"spanId\":\"[0-9a-f]{16}\",\"parentId\":\"def\","
            + "\"service\":\"TestService\",\"operation\":\"TestOperation\","
            + "\"start\":123,"
            + "\"baggage\":\\{\"[ab]\":\"[ab]v\",\"[ab]\":\"[ab]v\"\\},"
            + "\"log\":\\{\"timestamp\":123,\"event\":\"Start-Span\"\\}\\}\n";

    assertThat(encoded, matchesPattern(pattern));
  }

  @Test
  public void testNewFinished() {
    Span span = new Span(defaultTracer(),
        "TestService",
        "TestOperation",
        123000,
        null,
        null);
    span.finish(133000);
    String encoded = new String(new JsonEncoder().encodeToBytes(span.reportable()));

    String pattern =
        "\\{\"traceId\":\"[0-9a-f]{16}\",\"spanId\":\"[0-9a-f]{16}\","
            + "\"service\":\"TestService\",\"operation\":\"TestOperation\","
            + "\"start\":123,\"finish\":133,\"duration\":10,"
            + "\"log\":\\{\"timestamp\":133,\"event\":\"Stop-Span\"\\}\\}\n";

    assertThat(encoded, matchesPattern(pattern));
  }

  @Test
  public void testNewFinishedWithSingleEvent() {
    Span span = new Span(singleEventTracer(),
        "TestService",
        "TestOperation",
        123000,
        null,
        null);
    span.finish(133000);
    String encoded = new String(new JsonEncoder().encodeToBytes(span.reportable()));

    String pattern =
        "\\{\"traceId\":\"[0-9a-f]{16}\",\"spanId\":\"[0-9a-f]{16}\","
            + "\"service\":\"TestService\",\"operation\":\"TestOperation\","
            + "\"start\":123,\"finish\":133,\"duration\":10,"
            + "\"logs\":\\[\\{\"timestamp\":123,\"event\":\"Start-Span\"\\},"
            + "\\{\"timestamp\":133,\"event\":\"Stop-Span\"\\}\\]\\}\n";

    assertThat(encoded, matchesPattern(pattern));
  }

}
