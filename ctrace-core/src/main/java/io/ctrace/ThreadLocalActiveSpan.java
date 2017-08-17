package io.ctrace;

import io.opentracing.ActiveSpanSource;
import io.opentracing.SpanContext;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link ThreadLocalActiveSpan} is a simple {@link io.opentracing.ActiveSpan} implementation that
 * relies on Java's thread-local storage primitive.
 *
 * @see ActiveSpanSource
 * @see Tracer#activeSpan()
 */
public class ThreadLocalActiveSpan implements ActiveSpan {
  private static final ThreadLocal<ThreadLocalActiveSpan> snapshot = new ThreadLocal<>();

  private final Tracer tracer;
  private final Span wrapped;
  private final ThreadLocalActiveSpan toRestore;
  private final AtomicInteger refCount;

  ThreadLocalActiveSpan(Tracer tracer, Span wrapped, AtomicInteger refCount) {
    this.tracer = tracer;
    this.refCount = refCount;
    this.wrapped = wrapped;
    this.toRestore = snapshot.get();
    snapshot.set(this);
    tracer.logger().activate(wrapped);
  }

  public static ThreadLocalActiveSpan activeSpan() {
    return snapshot.get();
  }

  @Override
  public void deactivate() {
    if (snapshot.get() != this) {
      // This shouldn't happen if users call methods in the expected order. Bail out.
      return;
    }
    snapshot.set(toRestore);

    if (0 == refCount.decrementAndGet()) {
      wrapped.finish();
    }
  }

  @Override
  public Continuation capture() {
    return new Continuation();
  }

  @Override
  public SpanContext context() {
    return wrapped.context();
  }

  @Override
  public ActiveSpan setTag(String key, String value) {
    wrapped.setTag(key, value);
    return this;
  }

  @Override
  public ActiveSpan setTag(String key, boolean value) {
    wrapped.setTag(key, value);
    return this;
  }

  @Override
  public ActiveSpan setTag(String key, Number value) {
    wrapped.setTag(key, value);
    return this;
  }

  @Override
  public ActiveSpan log(Map<String, ?> fields) {
    wrapped.log(fields);
    return this;
  }

  @Override
  public ActiveSpan log(long timestampMicroseconds, Map<String, ?> fields) {
    wrapped.log(timestampMicroseconds, fields);
    return this;
  }

  @Override
  public ActiveSpan log(String event) {
    wrapped.log(event);
    return this;
  }

  @Override
  public ActiveSpan log(long timestampMicroseconds, String event) {
    wrapped.log(timestampMicroseconds, event);
    return this;
  }

  @Override
  public ActiveSpan log(String eventName, Object payload) {
    wrapped.log(eventName, payload);
    return this;
  }

  @Override
  public ActiveSpan log(long timestampMicroseconds, String eventName, Object payload) {
    wrapped.log(timestampMicroseconds, eventName, payload);
    return this;
  }

  @Override
  public ActiveSpan setBaggageItem(String key, String value) {
    wrapped.setBaggageItem(key, value);
    return this;
  }

  @Override
  public String getBaggageItem(String key) {
    return wrapped.getBaggageItem(key);
  }

  @Override
  public ActiveSpan setOperationName(String operationName) {
    wrapped.setOperationName(operationName);
    return this;
  }

  @Override
  public void close() {
    deactivate();
  }

  @Override
  public Span span() {
    return this.wrapped;
  }

  private final class Continuation implements io.opentracing.ActiveSpan.Continuation {
    Continuation() {
      refCount.incrementAndGet();
    }

    @Override
    public ActiveSpan activate() {
      return new ThreadLocalActiveSpan(tracer, wrapped, refCount);
    }
  }

}
