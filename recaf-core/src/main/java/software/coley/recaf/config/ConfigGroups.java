package software.coley.recaf.config;

import software.coley.recaf.services.Service;

/**
 * Constants for {@link ConfigContainer#getGroup()}.
 *
 * @author Matt Coley
 */
public final class ConfigGroups {
	/**
	 * Used to split group into sections.
	 */
	public static final String PACKAGE_SPLIT = ".";
	/**
	 * Group base for {@link Service} classes.
	 */
	public static final String SERVICE = "service";
	/**
	 * Group for analyzing components.
	 */
	public static final String SERVICE_ANALYSIS = SERVICE + PACKAGE_SPLIT + "analysis";
	/**
	 * Group for compiler components.
	 */
	public static final String SERVICE_COMPILE = SERVICE + PACKAGE_SPLIT + "compile";
	/**
	 * Group for debug/attach components.
	 */
	public static final String SERVICE_DEBUG = SERVICE + PACKAGE_SPLIT + "debug";
	/**
	 * Group for decompiler components.
	 */
	public static final String SERVICE_DECOMPILE = SERVICE + PACKAGE_SPLIT + "decompile";
	/**
	 * Group for IO components.
	 */
	public static final String SERVICE_IO = SERVICE + PACKAGE_SPLIT + "io";
	/**
	 * Group for mapping components.
	 */
	public static final String SERVICE_MAPPING = SERVICE + PACKAGE_SPLIT + "mapping";
	/**
	 * Group for plugin components.
	 */
	public static final String SERVICE_PLUGIN = SERVICE + PACKAGE_SPLIT + "plugin";
	/**
	 * Group base for UI classes.
	 */
	public static final String SERVICE_UI = SERVICE + PACKAGE_SPLIT + "ui";
	/**
	 * Group for 3rd party.
	 * <br>
	 * Plugin registering new {@link ConfigContainer} instances should use this as the {@link ConfigContainer#getGroup()}.
	 * This group is given special treatment in the UI.
	 */
	public static final String EXTERNAL = "external";

	private ConfigGroups() {
	}

	/**
	 * @param container
	 * 		Container to get packages from.
	 *
	 * @return Group packages.
	 */
	public static String[] getGroupPackages(ConfigContainer container) {
		return container.getGroup().split('\\' + PACKAGE_SPLIT);
	}
}
