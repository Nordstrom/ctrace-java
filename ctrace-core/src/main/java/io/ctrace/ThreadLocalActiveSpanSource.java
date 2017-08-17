package io.ctrace;

import java.util.concurrent.atomic.AtomicInteger;

public class ThreadLocalActiveSpanSource implements ActiveSpanSource {

  private final Tracer tracer;

  public ThreadLocalActiveSpanSource(Tracer tracer) {
    this.tracer = tracer;
  }

  @Override
  public ThreadLocalActiveSpan activeSpan() {
    return ThreadLocalActiveSpan.activeSpan();
  }

  @Override
  public ActiveSpan makeActive(Span span) {
    return new ThreadLocalActiveSpan(this.tracer, span, new AtomicInteger(1));
  }
}
