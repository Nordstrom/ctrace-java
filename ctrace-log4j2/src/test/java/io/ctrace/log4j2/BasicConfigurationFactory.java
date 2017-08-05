package io.ctrace.log4j2;

import java.net.URI;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;


public class BasicConfigurationFactory extends ConfigurationFactory {
  @Override
  public Configuration getConfiguration(
      final LoggerContext loggerContext,
      final ConfigurationSource source) {
    return null;
  }

  @Override
  public Configuration getConfiguration(
      final LoggerContext loggerContext,
      final String name,
      final URI configLocation) {
    return new BasicConfiguration();
  }

  @Override
  public String[] getSupportedTypes() {
    return null;
  }

  class BasicConfiguration extends AbstractConfiguration {

    private static final String DEFAULT_LEVEL = "org.apache.logging.log4j.level";

    BasicConfiguration() {
      super(null, ConfigurationSource.NULL_SOURCE);

      final LoggerConfig root = getRootLogger();
      final String name = System.getProperty(DEFAULT_LEVEL);
      final Level level =
          (name != null && Level.getLevel(name) != null) ? Level.getLevel(name) : Level.ERROR;
      root.setLevel(level);
    }
  }
}
