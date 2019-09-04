package me.coley.recaf.parse.assembly;

import me.coley.recaf.parse.assembly.visitors.AliasVisitor;
import me.coley.recaf.parse.assembly.visitors.AssemblyVisitor;

import java.util.HashMap;
import java.util.Map;

/**
 * Alias manager.
 *
 * @author Matt
 */
public class Aliases {
	private Map<String, String> aliases = new HashMap<>();

	/**
	 * Remove stored aliases.
	 */
	public void reset() {
		aliases.clear();
	}

	/**
	 * @param name
	 * 		Key alias.
	 * @param content
	 * 		Alias content.
	 */
	public void add(String name, String content) {
		aliases.put(name, content);
	}

	/**
	 * Replace aliases in the code. <br>
	 * <b>Note:</b> This logic exists here because it must be all done before the standard
	 * {@link Visitor#visitPre(Object)} pass.
	 *
	 * @param asm
	 * 		Assembler instance.
	 * @param lines
	 * 		Code to assemble.
	 *
	 * @throws LineParseException
	 * 		When the visitor lookup fails, or if the {@link AliasVisitor#visitPre(String)} fails.
	 */
	public void update(AssemblyVisitor asm, String[] lines) throws LineParseException {
		// Parse alias lines
		for(int i = 0; i < lines.length; i++) {
			String tmpLine = lines[i];
			Visitor<String> visitor = asm.getVisitor(i + 1, tmpLine);
			if (visitor instanceof AliasVisitor)
				visitor.visitPre(tmpLine);
		}
		// Replace aliases
		for(int i = 0; i < lines.length; i++) {
			String tmpLine = lines[i];
			for(Map.Entry<String, String> e : aliases.entrySet()) {
				String key = "${" + e.getKey() + "}";
				String content = e.getValue();
				if(tmpLine.contains(key))
					tmpLine = tmpLine.replace(key, content);
			}
			lines[i] = tmpLine;
		}
	}

}
