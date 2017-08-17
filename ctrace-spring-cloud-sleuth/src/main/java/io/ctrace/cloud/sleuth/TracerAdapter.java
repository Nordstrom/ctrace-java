package io.ctrace.cloud.sleuth;

import io.ctrace.Tracer;
import java.util.concurrent.Callable;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Sampler;
import org.springframework.cloud.sleuth.Span;

public class TracerAdapter implements org.springframework.cloud.sleuth.Tracer {

  private Tracer tracer;

  @Autowired
  public TracerAdapter(Tracer tracer) {
    this.tracer = tracer;
  }

  /**
   * Creates a new Span.
   * <p/>
   * If this thread has a currently active span, it will be the parent of the span we create here.
   * If there is no currently active trace span, the trace scope we create will be empty.
   *
   * @param name The name field for the new span to create.
   */
  @Override
  public Span createSpan(String name) {
    val span = this.tracer.buildSpan(name).startManual();
    val activeSpan = this.tracer.makeActive(span);
    return new SpanAdapter(this.tracer, span, activeSpan);
  }

  /**
   * Creates a new Span with a specific parent. The parent might be in another process or thread.
   * <p/>
   * If this thread has a currently active trace span, it must be the 'parent' span that you pass in
   * here as a parameter. The trace scope we create here will contain a new span which is a child of
   * 'parent'.
   *
   * @param name The name field for the new span to create.
   */
  @Override
  public Span createSpan(String name, Span parent) {
    // val span = this.tracer.buildSpan(name).asstartManual();
    // val activeSpan = this.tracer.makeActive(span);
    // return new SpanAdapter(this.tracer, span, activeSpan);
    return null;
  }

  /**
   * Start a new span if the sampler allows it or if we are already tracing in this thread. A
   * sampler can be used to limit the number of traces created.
   *
   * @param name the name of the span
   * @param sampler a sampler to decide whether to create the span or not
   */
  @Override
  public Span createSpan(String name, Sampler sampler) {
    return null;
  }

  /**
   * Contributes to a span started in another thread. The returned span shares mutable state with
   * the input.
   */
  @Override
  public Span continueSpan(Span span) {
    return null;
  }

  /**
   * Adds a tag to the current span if tracing is currently on. <p> Every span may also have zero or
   * more key/value Tags, which do not have timestamps and simply annotate the spans.
   *
   * Check {@link TraceKeys} for examples of most common tag keys
   */
  @Override
  public void addTag(String key, String value) {

  }

  /**
   * Remove this span from the current thread, but don't stop it yet nor send it for collection.
   * This is useful if the span object is then passed to another thread for use with {@link
   * Tracer#continueSpan(Span)}. <p> Example of usage:
   * <pre>{@code
   *     // Span "A" was present in thread "X". Let's assume that we're in thread "Y" to which span
   * "A" got passed
   *     Span continuedSpan = tracer.continueSpan(spanA);
   *     // Now span "A" got continued in thread "Y".
   *     ... // Some work is done... state of span "A" could get mutated
   *     Span previouslyStoredSpan = tracer.detach(continuedSpan);
   *     // Span "A" got removed from the thread Y but it wasn't yet sent for collection.
   *     // Additional work can be done on span "A" in thread "X" and finally it can get closed and
   * sent for collection
   *     tracer.close(spanA);
   * }</pre>
   *
   * @return the saved trace if there was one before the trace started (null otherwise)
   */
  @Override
  public Span detach(Span span) {
    return null;
  }

  /**
   * Remove this span from the current thread, stop it and send it for collection.
   *
   * @param span the span to close
   * @return the saved span if there was one before the trace started (null otherwise)
   */
  @Override
  public Span close(Span span) {
    return null;
  }

  /**
   * Returns a wrapped {@link Callable} which will be recorded as a span in the current trace.
   */
  @Override
  public <V> Callable<V> wrap(Callable<V> callable) {
    return null;
  }

  /**
   * Returns a wrapped {@link Runnable} which will be recorded as a span in the current trace.
   */
  @Override
  public Runnable wrap(Runnable runnable) {
    return null;
  }

  /**
   * Retrieves the span that is present in the context. If currently there is no tracing going on,
   * then this method will return {@code null}.
   */
  @Override
  public Span getCurrentSpan() {
    return null;
  }

  /**
   * Returns {@code true} when a span is present in the current context. In other words if a span
   * was started or continued then this method returns {@code true}.
   */
  @Override
  public boolean isTracing() {
    return false;
  }
}
