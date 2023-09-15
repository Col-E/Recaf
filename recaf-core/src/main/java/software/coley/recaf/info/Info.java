package software.coley.recaf.info;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.properties.PropertyContainer;

/**
 * Outline of all info types.
 *
 * @author Matt Coley
 */
public interface Info extends PropertyContainer {
	/**
	 * @return Name.
	 */
	@Nonnull
	String getName();

	/**
	 * @return Self cast to general class.
	 */
	@Nonnull
	ClassInfo asClass();

	/**
	 * @return Self cast to general file.
	 */
	@Nonnull
	FileInfo asFile();

	/**
	 * @return {@code true} if self is a general class.
	 */
	boolean isClass();

	/**
	 * @return {@code true} if self is a general file.
	 */
	boolean isFile();
}
