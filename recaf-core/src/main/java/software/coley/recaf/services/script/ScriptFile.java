package software.coley.recaf.services.script;

import jakarta.annotation.Nonnull;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Wrapper of a script path, with meta-data.
 *
 * @param path
 * 		Path to script file.
 * @param tags
 * 		Script's metadata tags.
 *
 * @author Matt Coley
 */
public record ScriptFile(@Nonnull Path path, @Nonnull String source,
						 @Nonnull Map<String, String> tags) implements Comparable<ScriptFile> {
	public static final String KEY_NAME = "name";
	public static final String KEY_DESCRIPTION = "description";
	public static final String KEY_VERSION = "version";
	public static final String KEY_AUTHOR = "author";

	/**
	 * Executes the script's content in the given engine.
	 *
	 * @param engine
	 * 		Engine to execute with.
	 *
	 * @return Script execution future.
	 */
	public CompletableFuture<ScriptResult> execute(@Nonnull ScriptEngine engine) {
		return engine.run(source());
	}

	/**
	 * @return Script name.
	 */
	public String name() {
		return getTagValue(KEY_NAME);
	}

	/**
	 * @return Script description.
	 */
	public String description() {
		return getTagValue(KEY_DESCRIPTION);
	}

	/**
	 * @return Script version.
	 */
	public String version() {
		return getTagValue(KEY_VERSION);
	}

	/**
	 * @return Script author.
	 */
	public String author() {
		return getTagValue(KEY_AUTHOR);
	}

	/**
	 * @param tag
	 * 		Name of tag.
	 *
	 * @return Value of tag, or empty string if no tag exists.
	 */
	@Nonnull
	public String getTagValue(@Nonnull String tag) {
		return tags.getOrDefault(tag, "");
	}

	@Override
	public int compareTo(ScriptFile o) {
		return path().compareTo(o.path());
	}
}
