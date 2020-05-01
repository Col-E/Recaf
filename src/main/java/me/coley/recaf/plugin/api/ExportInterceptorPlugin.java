package me.coley.recaf.plugin.api;

/**
 * Allow plugins to intercept and modify exported classes and files.
 *
 * @author Matt
 */
public interface ExportInterceptorPlugin extends BasePlugin {
	/**
	 * Intercept the given item. May be a class or file.
	 *
	 * @param name
	 * 		Name of item being exported.
	 * 		Typically is a full path, allowing identification based on file extensions.
	 * @param code
	 * 		Raw data of item.
	 *
	 * @return Raw data of item to export.
	 */
	byte[] intercept(String name, byte[] code);
}
