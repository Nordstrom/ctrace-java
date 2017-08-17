package io.ctrace;

import java.io.IOException;
import java.io.OutputStream;

/**
 * StreamLogger encodes and reports the span to an output stream.
 */
public class StreamLogger implements Logger {

  private OutputStream stream;
  private Encoder encoder;

  public StreamLogger(OutputStream stream) {
    this.stream = stream;
  }

  @Override
  public void init(Encoder encoder) {
    this.encoder = encoder;
  }

  @Override
  public void start(Span span, Log log) {
    this.write(span, log, false);
  }

  @Override
  public void finish(Span span, Log log) {
    this.write(span, log, true);
  }

  @Override
  public void activate(Span span) {}

  @Override
  public void log(Span span, Log log) {
    this.write(span, log, false);
  }

  private void write(Span span, Log log, boolean flush) {
    byte[] encoded = this.encoder.encodeToBytes(span, log);
    try {
      this.stream.write(encoded);
      if (flush) {
        this.stream.flush();
      }
    } catch (IOException e) {
      // Do nothing at this point...
    }
  }
}
