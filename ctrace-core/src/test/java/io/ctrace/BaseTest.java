package io.ctrace;

import java.util.ArrayList;
import java.util.List;

/**
 * Base for all unit test classes.
 */
public class BaseTest {

  protected TestLogger logger = new TestLogger();

  protected Tracer defaultTracer() {
    return Tracer
        .withLogger(logger)
        .withServiceName("TestService")
        .build();
  }

  protected Tracer singleEventTracer() {
    return Tracer
        .withLogger(logger)
        .withServiceName("TestService")
        .withSingleSpanOutput(true)
        .build();
  }

  public class TestLogger implements Logger {

    private boolean started;
    private boolean finished;
    private boolean logged;

    private ArrayList<Log> logs = new ArrayList<>();

    @Override
    public void start(Span span, Log log) {
      this.logs.add(log);
      this.started = true;
    }

    @Override
    public void finish(Span span, Log log) {
      this.logs.add(log);
      this.finished = true;
    }

    @Override
    public void log(Span span, Log log) {
      this.logs.add(log);
      this.logged = true;
    }

    public List<Log> logs() {
      return this.logs;
    }

    public boolean started() {
      return this.started;
    }

    public boolean finished() {
      return this.finished;
    }

    public boolean logged() {
      return this.logged;
    }
  }
}
