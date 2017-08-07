package io.ctrace.log4j2;

import static org.cthul.matchers.CthulMatchers.matchesPattern;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

import io.ctrace.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.val;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Test CtraceLayout.
 */
public class CtraceLayoutTest {

  private static ConfigurationFactory cf = new BasicConfigurationFactory();

  /**
   * Run after class tests.
   */
  @AfterClass
  public static void afterClass() {
    ConfigurationFactory.removeConfigurationFactory(cf);
    ThreadContext.clearAll();
  }

  /**
   * Run before class tests.
   */
  @BeforeClass
  public static void beforeClass() {
    ThreadContext.clearAll();
    ConfigurationFactory.setConfigurationFactory(cf);
    final LoggerContext ctx = LoggerContext.getContext();
    ctx.reconfigure();
  }

  private LoggerContext ctx = LoggerContext.getContext();
  private Logger rootLogger = this.ctx.getRootLogger();

  /**
   * Run before each test.
   */
  @Before
  public void before() {
    final Map<String, Appender> appenders = this.rootLogger.getAppenders();
    for (final Appender appender : appenders.values()) {
      this.rootLogger.removeAppender(appender);
    }
    ThreadContext.clearAll();
  }

  /**
   * Test getContentType.
   */
  @Test
  public void testContentType() {
    val layout = CtraceLayout.newBuilder().build();
    assertEquals("application/json; charset=UTF-8", layout.getContentType());
  }

  /**
   * Test getContentType.
   */
  @Test
  public void testWithCharset() {
    val layout = CtraceLayout.newBuilder().withCharset(StandardCharsets.US_ASCII).build();
    assertEquals(StandardCharsets.US_ASCII, layout.getCharset());
    assertEquals("application/json; charset=US-ASCII", layout.getContentType());
  }

  /**
   * Test getContentType.
   */
  @Test
  public void testWithConfiguration() {
    val configuration = new DefaultConfiguration();
    val layout = CtraceLayout.newBuilder().withConfiguration(configuration).build();
    assertSame(configuration, layout.getConfiguration());
  }

  /**
   * Test CtraceLayout with full span.
   */
  @Test
  public void testFullSpan() {
    val layout = CtraceLayout.newBuilder().build();
    val appender = new InMemoryAppender(layout);
    appender.start();

    this.rootLogger.addAppender(appender);
    this.rootLogger.setLevel(Level.DEBUG);

    ThreadContext.put(Keys.TRACE_ID, "abc");
    ThreadContext.put(Keys.SPAN_ID, "def");
    ThreadContext.put(Keys.PARENT_ID, "ghi");
    ThreadContext.put(Keys.START, "123");
    ThreadContext.put(Keys.FINISH, "124");
    ThreadContext.put(Keys.DURATION, "1");
    ThreadContext.put(Keys.SERVICE, "svc");
    ThreadContext.put(Keys.OPERATION, "op");
    ThreadContext.put(Keys.TAGS, ",\"tags\":{\"t\":\"v\"}");
    ThreadContext.put(Keys.BAGGAGE, ",\"baggage\":{\"b\":\"v\"}");

    this.rootLogger.info("test message");

    appender.stop();
    val encoded = appender.toString();
    val pattern =
        "\\{\"traceId\":\"abc\",\"spanId\":\"def\",\"parentId\":\"ghi\","
            + "\"service\":\"svc\",\"operation\":\"op\","
            + "\"start\":123,\"finish\":124,\"duration\":1,"
            + "\"log\":\\{\"timestamp\":[0-9]{13},\"message\":\"test message\","
            + "\"level\":\"INFO\","
            + "\"event\":\"log\"\\},"
            + "\"tags\":\\{\"t\":\"v\"},"
            + "\"baggage\":\\{\"b\":\"v\"}\\}\n";

    assertThat(encoded, matchesPattern(pattern));
  }

  /**
   * Test CtraceLayout with minimal span.
   */
  @Test
  public void testMinimalSpan() {
    val layout = CtraceLayout.newBuilder().build();
    val appender = new InMemoryAppender(layout);
    appender.start();

    this.rootLogger.addAppender(appender);
    this.rootLogger.setLevel(Level.DEBUG);

    ThreadContext.put(Keys.TRACE_ID, "abc");
    ThreadContext.put(Keys.SPAN_ID, "def");
    ThreadContext.put(Keys.START, "123");
    ThreadContext.put(Keys.OPERATION, "op");

    this.rootLogger.debug("test message");

    appender.stop();
    val encoded = appender.toString();
    val pattern =
        "\\{\"traceId\":\"abc\",\"spanId\":\"def\","
            + "\"operation\":\"op\",\"start\":123,"
            + "\"log\":\\{\"timestamp\":[0-9]{13},\"message\":\"test message\","
            + "\"level\":\"DEBUG\",\"event\":\"log\"\\}\\}\n";

    assertThat(encoded, matchesPattern(pattern));
  }

  /**
   * Test CtraceLayout with no trace id.
   */
  @Test
  public void testNoTraceId() {
    val layout = CtraceLayout.newBuilder().build();
    val appender = new InMemoryAppender(layout);
    appender.start();

    this.rootLogger.addAppender(appender);
    this.rootLogger.setLevel(Level.DEBUG);

    this.rootLogger.info("test message");
    appender.stop();
    val encoded = appender.toString();
    assertEquals("CTRACE: Missing Trace Context", encoded);
  }

  /**
   * Test CtraceLayout with error.
   */
  @Test
  public void testError() {
    val layout = CtraceLayout.newBuilder().build();
    val appender = new InMemoryAppender(layout);
    appender.start();

    this.rootLogger.addAppender(appender);
    this.rootLogger.setLevel(Level.DEBUG);

    ThreadContext.put(Keys.TRACE_ID, "abc");
    ThreadContext.put(Keys.SPAN_ID, "def");
    ThreadContext.put(Keys.START, "123");
    ThreadContext.put(Keys.OPERATION, "op");

    try {
      throw new Exception("test exception");
    } catch (Exception e) {
      this.rootLogger.error("error message", e);
    }

    appender.stop();
    val encoded = appender.toString();
    val pattern =
        "\\{\"traceId\":\"abc\",\"spanId\":\"def\","
            + "\"operation\":\"op\",\"start\":123,"
            + "\"log\":\\{\"timestamp\":[0-9]{13},\"message\":\"error message\","
            + "\"level\":\"ERROR\","
            + "\"event\":\"error\","
            + "\"error.kind\":\"Throwable\","
            + "\"error.object\":\"java.lang.Exception: test exception\"\\}\\}\n";

    assertThat(encoded, matchesPattern(pattern));
  }
}
