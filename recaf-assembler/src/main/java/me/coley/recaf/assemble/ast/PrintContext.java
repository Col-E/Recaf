package me.coley.recaf.assemble.ast;

import me.coley.recaf.util.EscapeUtil;

public class PrintContext {
	public static final PrintContext DEFAULT_CTX = new PrintContext("");
	private final String keywordPrefix;

	public PrintContext(String keywordPrefix) {
		this.keywordPrefix = keywordPrefix;
	}

	public String getKeywordPrefix() {
		return keywordPrefix;
	}

	public String fmtKeyword(String text) {
		if (keywordPrefix == null || keywordPrefix.isEmpty())
			return text;
		return keywordPrefix + text;
	}

	/**
	 * Format a JVM identifier, such as a class name, field name, or method name.
	 * This method will escape any characters that are not allowed in assembler
	 * @param identifier Identifier to format.
	 * @return Formatted identifier.
	 */
	public String fmtIdentifier(String identifier) {
		return EscapeUtil.escapeNonValid(identifier);
	}
}
