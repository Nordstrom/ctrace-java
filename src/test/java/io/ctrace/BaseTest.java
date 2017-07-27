package io.ctrace;

/**
 * Base for all unit test classes.
 */
public class BaseTest {
    protected Tracer defaultTracer() {
        return Tracer
                .withReporter(new NoopReporter())
                .withServiceName("TestService")
                .build();
    }

    protected Tracer singleEventTracer() {
        return Tracer
                .withReporter(new NoopReporter())
                .withServiceName("TestService")
                .withSingleSpanOutput(true)
                .build();
    }

    private class NoopReporter implements Reporter {
        @Override
        public void report(Span span) {
        }

        @Override
        public void flush() {
        }

    }
}
