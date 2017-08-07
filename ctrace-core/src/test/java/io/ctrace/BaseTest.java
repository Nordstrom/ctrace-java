package io.ctrace;

/**
 * Base for all unit test classes.
 */
public class BaseTest {
  protected NoopReporter reporter = new NoopReporter();

  protected Tracer defaultTracer() {
    return Tracer
        .withReporter(reporter)
        .withServiceName("TestService")
        .build();
  }

  protected Tracer singleEventTracer() {
    return Tracer
        .withReporter(reporter)
        .withServiceName("TestService")
        .withSingleSpanOutput(true)
        .build();
  }

  public class NoopReporter implements Reporter {
    private boolean flushed;

    @Override
    public void report(Reportable reportable) {
    }

    @Override
    public void flush() {
      this.flushed = true;
    }

    public boolean flushed() {
      return this.flushed;
    }
  }
}
