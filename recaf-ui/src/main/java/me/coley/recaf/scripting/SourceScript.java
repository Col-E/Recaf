package me.coley.recaf.scripting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Objects;

/**
 * Script implementation for raw source.
 *
 * @author yapht
 * @author Matt Coley
 */
public class SourceScript extends Script {
	private final String source;

	SourceScript(String source) {
		this.source = Objects.requireNonNull(source, "Source cannot be null!");
		parseTags();
	}

	@Override
	protected BufferedReader reader() throws IOException {
		return new BufferedReader(new StringReader(source));
	}

	@Override
	public ScriptResult execute() {
		return ScriptEngine.execute(source);
	}

	@Override
	public String getName() {
		if (name == null) {
			String taggedName = getTag("name");
			if (taggedName != null)
				name = taggedName;
			else
				name = "Untitled script";
		}
		return name;
	}

	@Override
	public String getSource() {
		return source;
	}
}
