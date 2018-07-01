package io.ctrace;

import java.util.ArrayList;
import java.util.List;

/** Base for all unit test classes. */
class BaseTest {

  TestLogger logger = new TestLogger();

  Tracer.TracerBuilder defaultTracerBuilder() {
    return Tracer.builder().logger(logger).serviceName("TestService");
  }

  Tracer defaultTracer() {
    return defaultTracerBuilder().build();
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

    List<Log> logs() {
      return this.logs;
    }

    boolean started() {
      return this.started;
    }

    boolean finished() {
      return this.finished;
    }

    boolean logged() {
      return this.logged;
    }
  }
}
