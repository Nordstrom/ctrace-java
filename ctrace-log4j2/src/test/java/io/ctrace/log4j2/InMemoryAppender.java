package io.ctrace.log4j2;

import io.ctrace.log4j2.InMemoryAppender.InMemoryManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender;
import org.apache.logging.log4j.core.appender.OutputStreamManager;

/**
 * For testing.  In memory appender.
 */
class InMemoryAppender extends AbstractOutputStreamAppender<InMemoryManager> {

  InMemoryAppender(final Layout<? extends Serializable> layout) {
    super("memory", layout, null, false, true,
        new InMemoryManager("memory", layout, false));
  }

  /**
   * To string.
   *
   * @return string
   */
  @Override
  public String toString() {
    return getManager().toString();
  }

  static class InMemoryManager extends OutputStreamManager {

    InMemoryManager(final String name, final Layout<? extends Serializable> layout,
        final boolean writeHeader) {
      super(new ByteArrayOutputStream(), name, layout, writeHeader);
    }

    @Override
    public String toString() {
      try {
        return getOutputStream().toString();
      } catch (final IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }
}

