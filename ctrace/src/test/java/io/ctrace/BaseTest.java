package io.ctrace;

import java.util.ArrayList;
import java.util.List;

/** Base for all unit test classes. */
public class BaseTest {

  protected TestLogger logger = new TestLogger();

  protected Tracer defaultTracer() {
    return Tracer.builder().logger(logger).serviceName("TestService").build();
  }

  protected Tracer singleEventTracer() {
    return Tracer.builder().logger(logger).serviceName("TestService").build();
  }

  public class TestLogger implements Logger {

    private boolean started;
    private boolean finished;
    private boolean logged;
    private boolean activated;
    private Encoder encoder;

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

    @Override
    public void activate(Span span) {
      this.activated = true;
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
