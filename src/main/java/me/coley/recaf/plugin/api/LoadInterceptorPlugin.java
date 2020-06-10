package me.coley.recaf.plugin.api;

/**
 * Allow plugins to intercept and modify loaded classes and files.
 *
 * @author Matt
 */
public interface LoadInterceptorPlugin extends BasePlugin {
	/**
	 * Intercept the given class.
	 *
	 * @param name
	 * 		Internal name of class.
	 * @param code
	 * 		Raw bytecode of class.
	 *
	 * @return Bytecode of class to load.
	 */
	byte[] interceptClass(String name, byte[] code);

	/**
	 * Intercept the given class. This class however, cannot be parsed by ASM.
	 *
	 * @param entryName
	 * 		File name.
	 * @param code
	 * 		Raw bytecode of class.
	 *
	 * @return Bytecode of class to load.
	 */
	byte[] interceptInvalidClass(String entryName, byte[] code);

	/**
	 * Intercept the given file.
	 *
	 * @param entryName
	 * 		File name.
	 * @param value
	 * 		Raw data of file.
	 *
	 * @return Raw data to load.
	 */
	byte[] interceptFile(String entryName, byte[] value);
}
