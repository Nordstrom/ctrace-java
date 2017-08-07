package io.ctrace;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class KeysTest {
  @Test
  public void testTraceId() {
    assertEquals("ctrace-trace-id", Keys.TRACE_ID);
  }

  @Test
  public void testSpanId() {
    assertEquals("ctrace-span-id", Keys.SPAN_ID);
  }

  @Test
  public void testParentId() {
    assertEquals("ctrace-parent-id", Keys.PARENT_ID);
  }

  @Test
  public void testService() {
    assertEquals("ctrace-service", Keys.SERVICE);
  }

  @Test
  public void testOperation() {
    assertEquals("ctrace-operation", Keys.OPERATION);
  }

  @Test
  public void testStart() {
    assertEquals("ctrace-start", Keys.START);
  }

  @Test
  public void testFinish() {
    assertEquals("ctrace-finish", Keys.FINISH);
  }

  @Test
  public void testDuration() {
    assertEquals("ctrace-duration", Keys.DURATION);
  }

  @Test
  public void testTags() {
    assertEquals("ctrace-tags", Keys.TAGS);
  }

  @Test
  public void testBaggage() {
    assertEquals("ctrace-bag", Keys.BAGGAGE);
  }
}
