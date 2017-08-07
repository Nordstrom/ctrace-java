package io.ctrace;

import static org.cthul.matchers.CthulMatchers.matchesPattern;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import lombok.val;
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
                    null))));

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
                    null))));

    String pattern =
        "\\{\"traceId\":\"abc\",\"spanId\":\"[0-9a-f]{16}\",\"parentId\":\"def\","
            + "\"service\":\"TestService\",\"operation\":\"TestOperation\","
            + "\"start\":123,"
            + "\"log\":\\{\"timestamp\":123,\"event\":\"Start-Span\"\\},"
            + "\"tags\":\\{\"t1\":\"tval1\",\"t2\":55,\"t3\":true\\}\\}\n";

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
                new SpanContext("abc", "def", bag))));

    String pattern =
        "\\{\"traceId\":\"abc\",\"spanId\":\"[0-9a-f]{16}\",\"parentId\":\"def\","
            + "\"service\":\"TestService\",\"operation\":\"TestOperation\","
            + "\"start\":123,"
            + "\"log\":\\{\"timestamp\":123,\"event\":\"Start-Span\"\\},"
            + "\"baggage\":\\{\"[ab]\":\"[ab]v\",\"[ab]\":\"[ab]v\"\\}\\}\n";

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
    String encoded = new String(new JsonEncoder().encodeToBytes(span));

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
    String encoded = new String(new JsonEncoder().encodeToBytes(span));

    String pattern =
        "\\{\"traceId\":\"[0-9a-f]{16}\",\"spanId\":\"[0-9a-f]{16}\","
            + "\"service\":\"TestService\",\"operation\":\"TestOperation\","
            + "\"start\":123,\"finish\":133,\"duration\":10,"
            + "\"logs\":\\[\\{\"timestamp\":123,\"event\":\"Start-Span\"\\},"
            + "\\{\"timestamp\":133,\"event\":\"Stop-Span\"\\}\\]\\}\n";

    assertThat(encoded, matchesPattern(pattern));
  }

  @Test
  public void testEncodeTags() {
    Map<String, Object> tags = new HashMap<>();
    tags.put("t1", "tval1");
    tags.put("t2", 55);
    tags.put("t3", true);
    String encoded = new JsonEncoder()
        .encodeTags(new Span(defaultTracer(),
            "TestService",
            "TestOperation",
            123000,
            tags,
            new SpanContext("abc",
                "def",
                null)));

    String pattern = ",\"tags\":\\{\"t1\":\"tval1\",\"t2\":55,\"t3\":true\\}";

    assertThat(encoded, matchesPattern(pattern));
  }

  @Test
  public void testEncodeBaggage() {
    Map<String, String> bag = new HashMap<>();
    bag.put("a", "av");
    bag.put("b", "bv");
    String encoded = new JsonEncoder()
        .encodeBaggage(new Span(defaultTracer(),
            "TestService",
            "TestOperation",
            123000,
            null,
            new SpanContext("abc", "def", bag)));

    String pattern = ",\"baggage\":\\{\"[ab]\":\"[ab]v\",\"[ab]\":\"[ab]v\"\\}";
    assertThat(encoded, matchesPattern(pattern));
  }

  @Test
  public void testWithEncodedTags() {
    val r = new TestReportable(
        "abc",
        "def",
        null,
        "op",
        null,
        123,
        0,
        0,
        ",\"tags\":{\"t\":\"v\"}",
        null);
    val encoded = new JsonEncoder()
        .encodeToString(r);

    val pattern =
        "\\{\"traceId\":\"abc\",\"spanId\":\"def\",\"operation\":\"op\","
            + "\"start\":123,\"tags\":\\{\"t\":\"v\"}\\}\n";

    assertThat(encoded, matchesPattern(pattern));
  }

  @Test
  public void testWithEncodedBaggage() {
    val r = new TestReportable(
        "abc",
        "def",
        null,
        "op",
        null,
        123,
        0,
        0,
        null,
        ",\"baggage\":{\"b\":\"v\"}");
    val encoded = new JsonEncoder()
        .encodeToString(r);

    val pattern =
        "\\{\"traceId\":\"abc\",\"spanId\":\"def\",\"operation\":\"op\","
            + "\"start\":123,\"baggage\":\\{\"b\":\"v\"}\\}\n";

    assertThat(encoded, matchesPattern(pattern));
  }


  private static class TestReportable extends AbstractReportable {

    private String traceId;
    private String spanId;
    private String encodedTags;
    private String encodedBaggage;

    TestReportable(
        String traceId,
        String spanId,
        String serviceName,
        String operationName,
        String parentId,
        long startMillis,
        long finishMillis,
        long duration,
        String encodedTags,
        String encodedBaggage) {
      super(
          serviceName,
          operationName,
          parentId,
          startMillis,
          finishMillis,
          duration,
          null,
          null,
          null);
      this.traceId = traceId;
      this.spanId = spanId;
      this.encodedTags = encodedTags;
      this.encodedBaggage = encodedBaggage;
    }

    @Override
    public String traceId() {
      return this.traceId;
    }

    @Override
    public String spanId() {
      return this.spanId;
    }

    @Override
    public String encodedTags() {
      return this.encodedTags;
    }

    @Override
    public String encodedBaggage() {
      return this.encodedBaggage;
    }

    @Override
    public Iterable<Entry<String, String>> baggage() {
      return null;
    }
  }
}
