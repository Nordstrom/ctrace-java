package io.ctrace;

/**
 * Logger encodes and logs the span to an output or transport.
 */
public interface Logger {
  void init(Encoder encoder);
  void start(Span span, Log log);
  void activate(Span span);
  void finish(Span span, Log log);
  void log(Span span, Log log);
}
