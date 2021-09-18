package me.coley.recaf.config;

/**
 * Container of config values, marked with {@link ConfigID}.
 *
 * @author Matt Coley
 */
public interface ConfigContainer {
	/**
	 * @return A file path <i>(Internal to the jar)</i> to an icon that represents the container.
	 * May be {@code null}.
	 */
	String iconPath();

	/**
	 * @return Display name for the {@link #internalName() internal name} to show in UI's.
	 */
	String displayName();

	/**
	 * @return Internal name of contained config items. Used as a prefix for items marked with {@link ConfigID}.
	 */
	String internalName();

	/**
	 * @return {@code true} when the config container is not meant to be user-facing. They can be omitted from display.
	 */
	default boolean isInternal() {
		return false;
	}
}
