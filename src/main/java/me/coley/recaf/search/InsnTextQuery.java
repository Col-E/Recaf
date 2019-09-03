package me.coley.recaf.search;

import me.coley.recaf.util.StringUtil;

import java.util.List;

/**
 * Query to find instructions based off of their disassembled representation.
 *
 * @author Matt
 */
public class InsnTextQuery extends Query {
	private final List<String> lines;

	/**
	 * Constructs a instruction text query.
	 *
	 * @param lines
	 * 		Consecutive lines to match.
	 * @param stringMode
	 * 		How to match strings.
	 */
	public InsnTextQuery(List<String> lines, StringMatchMode stringMode) {
		super(QueryType.INSTRUCTION_TEXT, stringMode);
		this.lines = lines;
	}

	/**
	 * Adds a result if the given class matches the specified name pattern.
	 *
	 * @param code
	 * 		Disassembled method code.
	 */
	public void match(String code) {
		String[] codeLines = StringUtil.splitNewline(code);
		int max = codeLines.length - lines.size();
		// Ensure search query is shorter than method code
		if (max <= 0)
			return;
		// Iterate over method code
		for (int i = 0; i < max; i++) {
			boolean match = true;
			// Iterate over query code
			// - Assert each line matches the query input
			// - If matching for all lines, return the match
			// - If a line doesn't match skip to the next method insn starting point
			for (int j = 0; j < lines.size(); j++) {
				String line = lines.get(j);
				String lineDis = codeLines[i+j];
				if (!stringMode.match(line, lineDis)) {
					match = false;
					break;
				}
			}
			if (match) {
				getMatched().add(new InsnResult(lines));
				return;
			}
		}
	}
}
