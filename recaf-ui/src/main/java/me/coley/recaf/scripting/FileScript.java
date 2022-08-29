package me.coley.recaf.scripting;

import me.coley.recaf.util.StringUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Script implementation for an external file.
 *
 * @author yapht
 * @author Matt Coley
 */
public class FileScript extends Script {
	private final Path path;

	FileScript(Path path) {
		this.path = Objects.requireNonNull(path, "Path cannot be null!");
		parseTags();
	}

	/**
	 * @return Script's source as a path
	 */
	public Path getPath() {
		return path;
	}

	@Override
	protected BufferedReader reader() throws IOException {
		return Files.newBufferedReader(path);
	}

	@Override
	public ScriptResult execute() {
		try {
			return ScriptEngine.execute(path);
		} catch (IOException ex) {
			throw new IllegalStateException("Failed to read from local script file", ex);
		}
	}

	@Override
	public String getName() {
		if (name == null) {
			String taggedName = getTag("name");
			if (taggedName != null)
				name = taggedName;
			else
				name = path.getFileName().toString();
		}
		return name;
	}

	@Override
	public String getSource() {
		String source;
		try {
			source = Files.readString(path);
		} catch (IOException e) {
			source = "// Failed to read script: " + path + "\n//" +
					StringUtil.traceToString(e).replace("\n", "\n// ");
		}
		return source;
	}
}
