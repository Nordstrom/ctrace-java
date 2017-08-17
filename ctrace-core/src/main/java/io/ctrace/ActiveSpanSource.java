package io.ctrace;

public interface ActiveSpanSource {

  ThreadLocalActiveSpan activeSpan();

  ActiveSpan makeActive(Span span);
}
