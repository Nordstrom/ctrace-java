package io.ctrace.log4j2;

import io.ctrace.Encoder;
import io.ctrace.JsonEncoder;
import io.ctrace.Log;
import io.ctrace.PreEncodedSpan;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.val;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;


public class CtraceLayout extends AbstractStringLayout {

  private static final Encoder encoder = new JsonEncoder();

  /**
   * Builds a new layout.
   *
   * @param config the configuration
   * @param charset the charset used to encode the header bytes, footer bytes and anything else
   *     that needs to be converted from strings to bytes.
   */
  private CtraceLayout(Configuration config, Charset charset) {
    super(config, charset, null, null);
  }

  /**
   * Create new builder.
   *
   * @return new builder
   */
  @PluginBuilderFactory
  public static Builder newBuilder() {
    return new Builder();
  }

  /**
   * Get Content Type.
   *
   * @return The default content type for Strings.
   */
  @Override
  public String getContentType() {
    return "application/json; charset=" + this.getCharset();
  }

  /**
   * Returns the header, if one is available.
   *
   * @return A byte array containing the header.
   */
  @Override
  public byte[] getHeader() {
    return null;
  }

  /**
   * Returns the footer, if one is available.
   *
   * @return A byte array containing the footer.
   */
  @Override
  public byte[] getFooter() {
    return null;
  }

  /**
   * Formats the event as an Object that can be serialized.
   *
   * @param event The Logging Event.
   * @return The formatted event.
   */
  @Override
  public String toSerializable(LogEvent event) {
    // We have several options for getting current tracing context we will try these in this order
    // 1. Get context from MDC - this is preferred as it decouples this layout from ctrace core
    //    entirely
    // 2. Get global active span.
    // We'll go with #1 for now...

    Map<String, Object> fields = new LinkedHashMap<>();

    fields.put("message",
        event.getMessage()
            .getFormattedMessage());
    fields.put("level", event.getLevel().name());
    Throwable t = event.getThrown();
    if (t != null) {
      fields.put("event", "error");
      fields.put("error.kind", "Throwable");
      fields.put("error.object", t.toString());
    } else {
      fields.put("event", "log");
    }

    // TODO: Add NDC ??
    val ctx = event.getContextData();
    val log = new Log(event.getTimeMillis(), fields);
    val encoded = new PreEncodedSpan(
        ctx.getValue(PreEncodedSpan.START),
        ctx.getValue(PreEncodedSpan.FINISH),
        ctx.getValue(PreEncodedSpan.TAGS),
        ctx.getValue(PreEncodedSpan.BAGGAGE)
    );

    if (encoded.start() == null || encoded.start().isEmpty()) {
      return "CTRACE: Missing Trace Context";
    }
    return encoder.encodeToString(encoded, log);
  }

  public static class Builder implements org.apache.logging.log4j.core.util.Builder<CtraceLayout> {

    @PluginBuilderAttribute
    private Charset charset = Charset.defaultCharset();

    @PluginConfiguration
    private Configuration configuration;

    /**
     * Build with charset.
     *
     * @param charset The character set. The platform default is used if not specified.
     */
    public Builder withCharset(final Charset charset) {
      if (charset != null) {
        this.charset = charset;
      }
      return this;
    }

    /**
     * Build with configuration.
     *
     * @param configuration The Configuration. Some Converters require access to the
     *     Interpolator.
     */
    public Builder withConfiguration(final Configuration configuration) {
      this.configuration = configuration;
      return this;
    }

    /**
     * Builds the object after all configuration has been set. This will use default values for any
     * unspecified attributes for the object.
     *
     * @return the configured instance.
     * @throws ConfigurationException if there was an error building the object.
     */
    @Override
    public CtraceLayout build() {
      if (configuration == null) {
        configuration = new DefaultConfiguration();
      }
      return new CtraceLayout(configuration, charset);
    }
  }
}
