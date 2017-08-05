package io.ctrace.log4j2;

import static org.junit.Assert.assertEquals;

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Test CtraceLayout.
 */
public class CtraceLayoutTest {
  static ConfigurationFactory cf = new BasicConfigurationFactory();

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

  /**
   * Test getContentType.
   */
  @Test
  public void testContentType() {
    final CtraceLayout layout = CtraceLayout.newBuilder().build();
    assertEquals("application/json; charset=UTF-8", layout.getContentType());
  }

  /**
   * Test default CtraceLayout.
   */
  @Test
  public void testCtraceLayout() {

  }
}
