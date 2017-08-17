package io.ctrace.log4j2;

import io.ctrace.Encoder;
import io.ctrace.Log;
import io.ctrace.Logger;
import io.ctrace.PreEncodedSpan;
import io.ctrace.Span;
import java.util.LinkedHashMap;
import lombok.val;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.message.MapMessage;

public class Log4J2Logger implements Logger {
  private static final org.apache.logging.log4j.Logger logger =
      LogManager.getLogger("ctrace");

  private Encoder encoder;
  private Level level;

  public Log4J2Logger() {
    this.level = Level.INFO;
  }

  public Log4J2Logger(Level level) {
    this.level = level;
  }

  @Override
  public void init(Encoder encoder) {
    this.encoder = encoder;
  }

  @Override
  public void start(Span span, Log log) {
    logger.log(level, logToMessage(log));
  }

  @Override
  public void activate(Span span) {
    this.putContext(span);
  }

  @Override
  public void finish(Span span, Log log) {
    logger.log(level, logToMessage(log));
  }

  @Override
  public void log(Span span, Log log) {
    logger.log(level, logToMessage(log));
  }

  private void putContext(Span span) {
    val encoded = this.encoder.preEncode(span);
    ThreadContext.put(PreEncodedSpan.START, encoded.start());
    ThreadContext.put(PreEncodedSpan.FINISH, encoded.finish());
    ThreadContext.put(PreEncodedSpan.TAGS, encoded.tags());
    ThreadContext.put(PreEncodedSpan.BAGGAGE, encoded.baggage());
  }

  // TODO:  For now we don't clear the context because it is not needed.
  private void clearContext() {
    ThreadContext.remove(PreEncodedSpan.START);
    ThreadContext.remove(PreEncodedSpan.FINISH);
    ThreadContext.remove(PreEncodedSpan.TAGS);
    ThreadContext.remove(PreEncodedSpan.BAGGAGE);
  }

  private MapMessage logToMessage(Log log) {
    val map = new LinkedHashMap<String, String>();
    for (val field : log.fields()) {
      map.put(field.key(), field.value().toString());
    }
    return new MapMessage(map);
  }
}
