package software.coley.recaf.info.properties.builtin;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.Info;
import software.coley.recaf.info.properties.BasicProperty;

import java.nio.file.Path;

/**
 * Built in property for associating a {@link Path} with some info type.
 *
 * @author Matt Coley
 */
public class InputFilePathProperty extends BasicProperty<Path> {
	public static final String KEY = "input-file-path";

	/**
	 * @param value
	 * 		Input path.
	 */
	public InputFilePathProperty(@Nonnull Path value) {
		super(KEY, value);
	}

	/**
	 * @param info
	 * 		Info instance.
	 *
	 * @return Input path associated with instance, or {@code null} when no association exists.
	 */
	@Nullable
	public static Path get(@Nonnull Info info) {
		return info.getPropertyValueOrNull(KEY);
	}

	/**
	 * @param info
	 * 		Info instance.
	 * @param inputPath
	 * 		Input path to associate with the item.
	 */
	public static void set(@Nonnull Info info, @Nonnull Path inputPath) {
		info.setProperty(new InputFilePathProperty(inputPath));
	}

	/**
	 * @param info
	 * 		Info instance.
	 */
	public static void remove(@Nonnull Info info) {
		info.removeProperty(KEY);
	}
}
