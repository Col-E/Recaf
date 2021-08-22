package me.coley.recaf.util;

import com.github.javaparser.Problem;
import com.github.javaparser.TokenRange;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import jregex.Matcher;
import jregex.Pattern;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

/**
 * Utility for basic code patches to make JavaParser parse more of the code.
 *
 * @author Matt
 * @author Andy Li
 */
public class JavaParserRecovery {
	private static final List<RecoveryStrategy> RECOVERY_STRATEGIES = new ArrayList<>();
	private static final String GROUP_LINE = "line";
	private static final String GROUP_COLUMN = "column";
	private static final Pattern PATTERN_LOCATION = RegexUtil.pattern("line ({" + GROUP_LINE + "}\\d+)," +
			" column ({" + GROUP_COLUMN + "}\\d+)");

	static {
		RECOVERY_STRATEGIES.add(JavaParserRecovery::recoverCFR);
		RECOVERY_STRATEGIES.add(JavaParserRecovery::recoverMissingSemicolon);
		RECOVERY_STRATEGIES.add(JavaParserRecovery::recoverMissingQuote);
		RECOVERY_STRATEGIES.add(JavaParserRecovery::recoverCurlyBraces);
	}

	/**
	 * Clean up the source code generated from decompiler and filter out the problems.
	 *
	 * @param code
	 * 		Source to clean up.
	 * @param problems
	 * 		Known problems.
	 *
	 * @return Cleaned source for parsing
	 */
	public static String filterDecompiledCode(String code, Collection<Problem> problems) {
		// Map the problems that prevent the lexer from doing a full parse.
		ListMultimap<Integer, LexicalError> lexerErrorMap = MultimapBuilder.ListMultimapBuilder
				.treeKeys()
				.arrayListValues().build();
		for (Problem problem : new HashSet<>(problems)) {
			String message = problem.getMessage();
			if (!problem.getLocation().isPresent() && message.contains("at line ")) {
				Matcher matcher = PATTERN_LOCATION.matcher(message);
				if (matcher.find()) {
					int line = Integer.parseInt(matcher.group(GROUP_LINE));
					int column = Integer.parseInt(matcher.group(GROUP_COLUMN));
					lexerErrorMap.put(line, new LexicalError(problem, line, column));
				}
			}
		}
		// Map the problems collection that were found after the lexer completed.
		// These should have more specific locations reported by JavaParser that are associated with AST nodes.
		ListMultimap<Integer, Problem> problemMap = MultimapBuilder.ListMultimapBuilder
				.treeKeys()
				.arrayListValues()
				.build(problems.stream()
						.filter(p -> p.getLocation().flatMap(TokenRange::toRange).isPresent())
						.collect(ImmutableListMultimap.toImmutableListMultimap(p ->
								p.getLocation().flatMap(TokenRange::toRange).get().begin.line, Function.identity())));
		// Rebuild the source with attempted fixes applied
		StringBuilder builder = new StringBuilder(code.length());
		try (LineNumberReader reader = new LineNumberReader(new StringReader(code))) {
			String line;
			while ((line = reader.readLine()) != null) {
				int lineNo = reader.getLineNumber();
				boolean commentOut = false;
				boolean custom = false;
				// Iterate over recovery strategies and attempt to find a fix to the line's problems.
				LineInfo lineInfo = new LineInfo(lineNo, line);
				for (RecoveryStrategy recoveryStrategy : RECOVERY_STRATEGIES) {
					RecoveryType recoveryType = recoveryStrategy.tryRecover(lineInfo, problemMap, lexerErrorMap);
					if (recoveryType == RecoveryType.LINE_COMMENT) {
						commentOut = true;
						break;
					} else if (recoveryType == RecoveryType.TEXT_EDIT) {
						custom = true;
						break;
					}
				}
				// Apply comment recovery if set
				if (commentOut) {
					// Insert line comment, and substring the line by '//'s length.
					// The user's wont see this so its OK. This balances out the change so context actions
					// do not occur at offset positions.
					builder.append("//");
					lineInfo.text = lineInfo.text.substring(2);
				} else if (custom) {
					// When a custom change is applies attempt to keep the document length the same.
					// We don't want document offsets where the user is requesting context actions to not represent
					// what is being parsed.
					//
					// This will screw up selection on the current line, but fixes it for the rest of the file.
					// Its an OK sacrifice in my opinion.
					int sizeDiff = line.length() - lineInfo.text.length();
					if (sizeDiff < 0) {
						// Line is longer than original, cut padding if possible.
						String prefix = lineInfo.text.substring(0, -sizeDiff);
						if (prefix.trim().length() == 0) {
							lineInfo.text = lineInfo.text.substring(-sizeDiff);
						}
					} else if (sizeDiff > 0) {
						// Line is shorter than original, add padding.
						for (int i = 0; i < sizeDiff; i++)
							builder.append(' ');
					}
				}
				builder.append(lineInfo.text).append('\n');
			}
		} catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
		return builder.toString();
	}


	private static RecoveryType recoverCFR(LineInfo line,
										   ListMultimap<Integer, Problem> problemMap,
										   ListMultimap<Integer, LexicalError> lexerErrorMap) {
		String trim = line.text.trim();
		// CFR is known to sometimes generate pseudocode that starts with **
		// Usually "** GOTO label", but can be other operations like "** continue;"
		// If the line starts with the "**" pattern we can just comment it out.
		if (trim.startsWith("** ")) {
			return RecoveryType.LINE_COMMENT;
		}
		// If it doesn't start with the content, we still want to patch it out.
		if (trim.contains("** continue;")) {
			line.text = line.text.replace("** continue;", "continue;");
			return RecoveryType.TEXT_EDIT;
		} else if (trim.contains("** case")) {
			line.text = line.text.replace("** case ", "case ");
			return RecoveryType.TEXT_EDIT;
		} else if (trim.contains("** GOTO ")) {
			// Appending the ';' because these pseudocode statements don't have them, and they always are the last
			// expression on a line (as far as I've seen) so its just simple to slap it on the end.
			line.text = line.text.replace("** GOTO ", "break ") + ";";
			return RecoveryType.TEXT_EDIT;
		}
		return RecoveryType.NONE;
	}

	private static RecoveryType recoverMissingSemicolon(LineInfo line,
														ListMultimap<Integer, Problem> problemMap,
														ListMultimap<Integer, LexicalError> lexerErrorMap) {
		List<Problem> lineProblems = problemMap.get(line.number);
		// Check for the an unfinished expression. The lexer doesn't say something like "oh I want a ';' here".
		// Instead it says "Oh expression, did you mean to assign that?"
		if (lineProblems.size() == 1 && lineProblems.stream()
					.map(Problem::getMessage)
					.anyMatch(m -> m.contains("Parse error. Found \"")
							&& m.contains("expected one of") && m.contains(">>>="))) {
			// Check if there is no ';'
			String trim = line.text.trim();
			if (trim.charAt(trim.length() - 1) != ';') {
				line.text += ";";
				return RecoveryType.TEXT_EDIT;
			}
		}
		return RecoveryType.NONE;
	}

	private static RecoveryType recoverMissingQuote(LineInfo line,
													ListMultimap<Integer, Problem> problemMap,
													ListMultimap<Integer, LexicalError> lexerErrorMap) {
		// Missing quotes come up as lexer problems
		List<LexicalError> lineProblems = lexerErrorMap.get(line.number);
		if (lineProblems.size() == 1) {
			// Check for 'encountered newline after ...' where '...' starts with an opening quote.
			LexicalError error = lineProblems.get(0);
			String msg = error.getMessage();
			if (msg.contains("Encountered: \"\\n\"") && msg.contains("after : \"\\\"")) {
				// Get the offending text
				String afterPattern = "after : \"\\";
				String afterOffendingText = msg.substring(
						msg.lastIndexOf(afterPattern) + afterPattern.length(),
						msg.length() - 1);
				int startOfProblem = line.text.indexOf(afterOffendingText);
				// Rebuild the line
				String originalText = line.text;
				// Estimate where the quote should be, probably before a ')' or ';'
				String suffix = originalText.substring(startOfProblem + 1);
				int end = suffix.indexOf(')');
				if (end < 0) {
					end = suffix.indexOf(';');
				}
				if (end > 0) {
					suffix = suffix.substring(end);
				}
				// To prevent offsetting things in the AST positions, make the quote size big enough
				// so the line length matches the old line length.
				int quoteContentLen = originalText.length() - startOfProblem - suffix.length() - 2;
				String quoteContent = Strings.repeat("?", Math.max(1, quoteContentLen));
				line.text = originalText.substring(0, startOfProblem) + "\"" + quoteContent + "\"" + suffix;
				return RecoveryType.TEXT_EDIT;
			}
		}
		return RecoveryType.NONE;
	}

	private static RecoveryType recoverCurlyBraces(LineInfo line,
												   ListMultimap<Integer, Problem> problemMap,
												   ListMultimap<Integer, LexicalError> lexerErrorMap) {
		// Check the current line
		List<Problem> lineProblems = problemMap.get(line.number);
		if (!lineProblems.isEmpty() && lineProblems.stream().map(Problem::getMessage)
					.noneMatch(m -> m.contains("expected \"}\"") || m.contains("expected \"{\""))) {
			return RecoveryType.LINE_COMMENT;
		}

		// Check the previous line
		List<Problem> priorLineProblems = problemMap.get(line.number - 1);
		if (!priorLineProblems.isEmpty() && priorLineProblems.stream().map(Problem::getMessage)
					.anyMatch(m -> m.contains("expected \"}\""))) {
			return RecoveryType.LINE_COMMENT;
		}

		// Check the next line
		List<Problem> nextLineProblems = problemMap.get(line.number + 1);
		if (!nextLineProblems.isEmpty() && nextLineProblems.stream().map(Problem::getMessage)
					.anyMatch(m -> m.contains("expected \"{\""))) {
			return RecoveryType.LINE_COMMENT;
		}

		// Does not apply here
		return RecoveryType.NONE;
	}

	/**
	 * Recovery implementation interface.
	 */
	interface RecoveryStrategy {
		RecoveryType tryRecover(LineInfo lineInfo,
								ListMultimap<Integer, Problem> problemMap,
								ListMultimap<Integer, LexicalError> lexerErrorMap);
	}

	/**
	 * Type of recovery applied.
	 */
	enum RecoveryType {
		LINE_COMMENT, TEXT_EDIT, NONE;
	}

	/**
	 * Wrapper for lexer errors that JavaParser can't handle in such a way to
	 * provide accurate location info with tokens.
	 */
	static class LexicalError {
		private final Problem problem;
		private final int line;
		private final int column;

		private LexicalError(Problem problem, int line, int column) {
			this.problem = problem;
			this.line = line;
			this.column = column;
		}

		String getMessage() {
			return problem.getMessage();
		}
	}

	/**
	 * Line info wrapper, allows editing of line's text.
	 */
	static class LineInfo {
		private final int number;
		private String text;

		public LineInfo(int number, String text) {
			this.number = number;
			this.text = text;
		}
	}
}
