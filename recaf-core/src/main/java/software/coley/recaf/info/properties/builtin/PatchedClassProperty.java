package software.coley.recaf.info.properties.builtin;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.Info;
import software.coley.recaf.info.properties.BasicProperty;
import software.coley.recaf.info.properties.Property;

import java.nio.file.Path;

/**
 * Built in property for indicating that a class's original bytes were rewritten on import.
 *
 * @author Matt Coley
 */
public class PatchedClassProperty extends BasicProperty<Boolean> {
	public static final PatchedClassProperty INSTANCE = new PatchedClassProperty();
	public static final String KEY = "patched-class";

	private PatchedClassProperty() {
		super(KEY, true);
	}

	@Override
	public boolean persistent() {
		return false;
	}

	/**
	 * @param info
	 * 		Info to check.
	 *
	 * @return {@code true} when the importer rewrote the original bytes of the class.
	 */
	public static boolean get(@Nonnull Info info) {
		Property<Boolean> property = info.getProperty(KEY);
		if (property != null) {
			Boolean value = property.value();
			return value != null && value;
		}
		return false;
	}

	/**
	 * Marks the info as a class whose original bytes were rewritten on import.
	 *
	 * @param info
	 * 		Info of the rewritten class.
	 */
	public static void set(@Nonnull Info info) {
		info.setProperty(INSTANCE);
	}
}
