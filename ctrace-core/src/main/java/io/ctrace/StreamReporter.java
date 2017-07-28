package io.ctrace;

import java.io.IOException;
import java.io.OutputStream;

/**
 * StreamReporter encodes and reports the span to an output stream.
 */
public class StreamReporter implements Reporter {
    private OutputStream stream;
    private Encoder encoder;

    public StreamReporter(OutputStream stream, Encoder encoder) {
        this.stream = stream;
        this.encoder = encoder;
    }

    public void report(Span span) {
        byte[] encoded = this.encoder.encode(span);
        try {
            this.stream.write(encoded);
        } catch (IOException e) {
            // Do nothing at this point...
        }
    }

    public void flush() {
        try {
            this.stream.flush();
        } catch (IOException e) {
            // do nothing
        }
    }
}
