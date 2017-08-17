package io.ctrace;

public interface ActiveSpan extends io.opentracing.ActiveSpan {
  Span span();
}
