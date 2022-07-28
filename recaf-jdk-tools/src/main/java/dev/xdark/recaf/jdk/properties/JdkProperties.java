package dev.xdark.recaf.jdk.properties;

import dev.xdark.recaf.jdk.ToolHelper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Data class containing all JDK properties,
 * such as environment variables, system properties, etc.
 *
 * @author xDark
 */
public final class JdkProperties {
	private static Path classpathJar;
	private final Map<String, String> systemProperties;
	private final Map<String, String> environment;

	/**
	 * @param systemProperties
	 * 		System properties.
	 * @param environment
	 * 		Environment variables.
	 */
	public JdkProperties(Map<String, String> systemProperties, Map<String, String> environment) {
		this.systemProperties = systemProperties;
		this.environment = environment;
	}

	/**
	 * @return system properties.
	 */
	public Map<String, String> getSystemProperties() {
		return systemProperties;
	}

	/**
	 * @return environment variables
	 */
	public Map<String, String> getEnvironment() {
		return environment;
	}

	/**
	 * @return JDK properties of current process.
	 */
	@SuppressWarnings("unchecked")
	public static JdkProperties current() {
		return new JdkProperties((Map<String, String>) (Map) System.getProperties()
				.entrySet()
				.stream()
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
				System.getenv());
	}

	/**
	 * Detects properties of the JDK.
	 *
	 * @param jdkExecutable
	 * 		Path to JDK executable.
	 *
	 * @return JDK properties.
	 *
	 * @throws IOException
	 * 		If any I/O error occurs.
	 */
	public static JdkProperties getProperties(Path jdkExecutable) throws IOException {
		if (!Files.isRegularFile(jdkExecutable)) {
			throw new IllegalStateException("JDK executable does not exist");
		}
		// Prepare jar
		Path classpathJar = JdkProperties.classpathJar;
		if (classpathJar == null) {
			classpathJar = Files.createTempFile("recaf-jdk-properties", ".jar");
			classpathJar.toFile().deleteOnExit();
			JdkProperties.classpathJar = classpathJar;
			ToolHelper.prepareJar(classpathJar, JdkPropertiesDump.class);
		}
		// Prepare process
		Path pipe = Files.createTempFile("recaf-jdk-pipe", ".bin");
		try {
			ProcessBuilder builder = new ProcessBuilder();
			builder.directory(jdkExecutable.getParent().toFile());
			builder.command(jdkExecutable.toString(), "-cp",
					classpathJar.toString(), JdkPropertiesDump.class.getName(), pipe.toString());
			// Start process & grab info
			// For format, see Main class
			Process process = builder.start();
			try {
				if (process.waitFor() != 0) {
					// Read error from stderr
					try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
						throw new IllegalStateException(reader.lines().collect(Collectors.joining("\n")));
					}
				}
				try (DataInputStream in = new DataInputStream(Files.newInputStream(pipe))) {
					int propertiesCount = in.readShort();
					Map<String, String> properties = new HashMap<>(propertiesCount);
					while (propertiesCount-- != 0) {
						properties.put(in.readUTF(), in.readUTF());
					}
					int envCount = in.readShort();
					Map<String, String> env = new HashMap<>(envCount);
					while (envCount-- != 0) {
						env.put(in.readUTF(), in.readUTF());
					}
					return new JdkProperties(properties, env);
				}
			} catch (InterruptedException e) {
				throw new IllegalStateException("Thread interrupted");
			} finally {
				close(process.getInputStream());
				close(process.getErrorStream());
				close(process.getOutputStream());
			}
		} finally {
			try {
				Files.deleteIfExists(pipe);
			} catch (IOException ignored) {
			}
		}
	}

	private static void close(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException ignored) {
			}
		}
	}
}
