package me.coley.recaf.scripting;

import jregex.Matcher;
import me.coley.recaf.util.RegexUtil;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an executable script.
 *
 * @author yapht
 */
public abstract class Script {
	private static final Logger logger = Logging.get(Script.class);
	public static final String EXTENSION = ".bsh";
	// Matches:
	//      @key   value
	private static final String TAG_PATTERN = "//(\\s+)?@({key}\\S+)\\s+({value}.+)";
	private final Map<String, String> tags = new HashMap<>();
	protected String name;

	/**
	 * @return Reader for the source.
	 *
	 * @throws IOException
	 * 		When the source cannot be read.
	 */
	protected abstract BufferedReader reader() throws IOException;

	/**
	 * Execute the script and return a result.
	 *
	 * @return Execution {@link ScriptResult result}.
	 */
	public abstract ScriptResult execute();

	/**
	 * @return Script's name from either metadata or filename
	 */
	public abstract String getName();

	/**
	 * @return Script's source code.
	 */
	public abstract String getSource();

	/**
	 * Parses the metadata tags.
	 */
	protected void parseTags() {
		// Stream in the lines, so we don't have load the entire file into memory
		try (BufferedReader reader = reader()) {
			String line;
			boolean started = false;
			while ((line = reader.readLine()) != null) {
				if (!line.startsWith("//"))
					continue;

				if (line.contains("==Metadata==")) {
					started = true;
					continue;
				}

				if (started && line.contains("==/Metadata=="))
					return; // End of metadata, we're done

				if (!started)
					continue;

				Matcher matcher = RegexUtil.getMatcher(TAG_PATTERN, line);
				if (matcher.matches()) {
					String key = matcher.group("key").toLowerCase();
					String value = matcher.group("value");
					tags.put(key, value);
				}
			}
		} catch (IOException ex) {
			logger.error("Failed to parse script tags", ex);
		}
	}

	/**
	 * Instantiate a new script from a file.
	 *
	 * @param path
	 * 		The script file path
	 *
	 * @return The script instance
	 */
	public static FileScript fromPath(Path path) {
		return new FileScript(path);
	}

	/**
	 * Instantiate a new script from raw source code.
	 *
	 * @param source
	 * 		The script source
	 *
	 * @return The script instance
	 */
	public static SourceScript fromSource(String source) {
		return new SourceScript(source);
	}

	/**
	 * Get a metadata tag by key.
	 *
	 * @param key
	 * 		The tag's key
	 *
	 * @return The tag or {@code null} if it doesn't exist
	 */
	public String getTag(String key) {
		return tags.getOrDefault(key, null);
	}
}
