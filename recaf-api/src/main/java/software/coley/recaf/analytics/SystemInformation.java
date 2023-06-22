package software.coley.recaf.analytics;

import org.slf4j.Logger;

import java.io.StringWriter;
import java.util.Map;
import java.util.TreeMap;

/**
 * Common properties pulled at runtime from {@link System#getProperty(String)}
 * that are useful for tracking and logging.
 *
 * @author Matt Coley
 */
public class SystemInformation {
	private static final String KEY_OS_NAME = "os.name";
	private static final String KEY_OS_ARCH = "os.arch";
	private static final String KEY_OS_VERSION = "os.version";
	private static final String KEY_OS_ARCH_BITS = "os.bitness";
	private static final String KEY_OS_PROCESSORS = "os.processors";
	private static final String KEY_JAVA_VERSION = "java.version";
	private static final String KEY_JAVA_VM_NAME = "java.vm.version";
	private static final String KEY_JAVA_VM_VENDOR = "java.vm.vendor";
	private static final String KEY_JAVA_HOME = "java.home";
	public static final String OS_NAME = System.getProperty(KEY_OS_NAME);
	public static final String OS_ARCH = System.getProperty(KEY_OS_ARCH);
	public static final int OS_ARCH_BITS = determineBitness();
	public static final String OS_VERSION = System.getProperty(KEY_OS_VERSION);
	public static final String JAVA_VERSION = System.getProperty(KEY_JAVA_VERSION);
	public static final String JAVA_VM_NAME = System.getProperty(KEY_JAVA_VM_NAME);
	public static final String JAVA_VM_VENDOR = System.getProperty(KEY_JAVA_VM_VENDOR);
	public static final String JAVA_HOME = System.getProperty(KEY_JAVA_HOME);
	private static final Map<String, String> ALL_PROPERTIES = new TreeMap<>() {
		{
			put(KEY_OS_NAME, OS_NAME);
			put(KEY_OS_ARCH, OS_ARCH);
			put(KEY_OS_ARCH_BITS, String.valueOf(OS_ARCH_BITS));
			put(KEY_OS_PROCESSORS, String.valueOf(Runtime.getRuntime().availableProcessors()));
			put(KEY_OS_VERSION, OS_VERSION);
			put(KEY_JAVA_VERSION, JAVA_VERSION);
			put(KEY_JAVA_VM_NAME, JAVA_VM_NAME);
			put(KEY_JAVA_VM_VENDOR, JAVA_VM_VENDOR);
			put(KEY_JAVA_HOME, JAVA_HOME);
		}
	};


	/**
	 * @return {@code 64} or {@code 32} dependent on the {@link #OS_ARCH}.
	 */
	private static int determineBitness() {
		// Parse from specification value, which is commonly defined.
		String bitness = System.getProperty("sun.arch.data.model", "");
		if (bitness.matches("[0-9]{2}"))
			return Integer.parseInt(bitness, 10);
		// Parse from IBM value, used on IBM releases.
		bitness = System.getProperty("com.ibm.vm.bitmode", "");
		if (bitness.matches("[0-9]{2}"))
			return Integer.parseInt(bitness, 10);
		return OS_ARCH.contains("64") ? 64 : 32;
	}

	/**
	 * Dump all properties into the given logger.
	 *
	 * @param logger
	 * 		Logger to dump into.
	 */
	public static void dump(Logger logger) {
		if (logger != null)
			ALL_PROPERTIES.forEach((key, value) ->
					logger.debug("{} = {}", key, value));
	}

	/**
	 * Dump all properties into the given writer.
	 *
	 * @param writer
	 * 		Writer to dump into.
	 */
	public static void dump(StringWriter writer) {
		if (writer != null)
			ALL_PROPERTIES.forEach((key, value) ->
					writer.append(String.format("%s = %s\n", key, value)));
	}
}
