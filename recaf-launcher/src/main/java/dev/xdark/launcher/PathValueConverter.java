package dev.xdark.launcher;

import java.nio.file.Path;
import java.nio.file.Paths;
import joptsimple.ValueConverter;

/**
 * Converts string to {@link Path}.
 *
 * @author xDark
 */
final class PathValueConverter implements ValueConverter<Path> {

  @Override
  public Path convert(String value) {
    return Paths.get(value);
  }

  @Override
  public Class<? extends Path> valueType() {
    return Path.class;
  }

  @Override
  public String valuePattern() {
    return null;
  }
}
