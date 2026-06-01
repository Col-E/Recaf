package software.coley.recaf.ui.control.richtext.suggest.java;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.util.RegexUtil;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Pattern;

/**
 * Parser for determining the lexical completion context at the current caret position.
 *
 * @author Matt Coley
 */
public class JavaLexicalContextParser {
	private static final Pattern TYPE_HEADER_PATTERN = Pattern.compile(".*\\b(class|interface|enum|record)\\b[^;{}]*$");
	private static final Pattern METHOD_HEADER_PATTERN = Pattern.compile(".*\\)\\s*(throws\\s+[^;{}]*)?$");
	private static final Pattern EXPRESSION_LIKE_PATTERN = Pattern.compile(".*(?:=|\\(|\\[|,|->|return|throw|case)\\s*$");
	private static final Pattern EXECUTABLE_BLOCK_PATTERN =
			Pattern.compile(".*(?:\\)\\s*(throws\\s+[^;{}]*)?|\\b(?:else|do|try|finally|switch|for|while|if|catch|synchronized|static)\\b[^;{}]*)$");

	/**
	 * Attempts to determine the lexical context at the given caret position within the provided Java source text.
	 *
	 * @param text
	 * 		Full text of the Java source.
	 * @param caret
	 * 		Caret position within the text to analyze.
	 *
	 * @return Lexical context at the caret position, or {@link JavaLexicalContext#none()} if no meaningful context could be determined.
	 */
	@Nonnull
	public JavaLexicalContext parse(@Nonnull String text, int caret) {
		// Skip if the caret is out of bounds, or not in code (IE, not any other source state like string literal, comments, etc).
		if (caret < 0 || caret > text.length())
			return JavaLexicalContext.none();
		if (caret == 0)
			return JavaLexicalContext.none();
		if (sourceStateAt(text, caret) != SourceState.CODE)
			return JavaLexicalContext.none();

		// Look back to the start of the current line to determine the context.
		int lineStart = 1 + text.lastIndexOf('\n', Math.max(0, caret - 1));
		String linePrefix = text.substring(lineStart, caret);
		if (linePrefix.isBlank())
			return JavaLexicalContext.none();

		// Handle some special contexts first, like imports and package declarations.
		String trimmed = linePrefix.stripLeading();
		if (trimmed.startsWith("import ")) {
			String importRemainder = trimmed.substring("import ".length());
			if (importRemainder.startsWith("static "))
				importRemainder = importRemainder.substring("static ".length());
			return new JavaLexicalContext(ContextKind.IMPORT, importRemainder.trim(), "", -1, false, KeywordSite.UNKNOWN);
		}
		if (trimmed.startsWith("package "))
			return new JavaLexicalContext(ContextKind.PACKAGE, trimmed.substring("package ".length()).trim(), "", -1, false, KeywordSite.UNKNOWN);

		// Next check for member access contexts, which are the most complex.
		// This includes both method references and member accesses.
		JavaLexicalContext memberContext = extractMemberContext(linePrefix, lineStart);
		if (memberContext.kind() != ContextKind.NONE)
			return memberContext;

		// Finally, if we aren't in any special contexts, we can try to determine if we're looking at a type or an identifier.
		String qualifiedPartial = trailingQualifiedName(linePrefix);
		String simplePartial = trailingIdentifier(linePrefix);
		String beforeSimple = linePrefix.substring(0, linePrefix.length() - simplePartial.length());
		if (isAnnotationContext(beforeSimple))
			return new JavaLexicalContext(ContextKind.TYPE, simplePartial, "", -1, true, KeywordSite.UNKNOWN);
		if (isTypeContext(beforeSimple))
			return new JavaLexicalContext(ContextKind.TYPE, qualifiedPartial, "", -1, false, KeywordSite.UNKNOWN);
		if (!simplePartial.isEmpty())
			return new JavaLexicalContext(ContextKind.IDENTIFIER, simplePartial, "", -1, false,
					inferKeywordSite(text, caret, simplePartial));

		// Nothing matched.
		return JavaLexicalContext.none();
	}

	/**
	 * Given a partial text that the user has typed and a full text that represents the completion,
	 * determine the suffix that should be inserted to complete the text.
	 *
	 * @param partialText
	 * 		The text that the user has currently typed, which may be a prefix of the full text.
	 * @param fullText
	 * 		The complete text that represents the intended completion.
	 * 		This is the text that should be inserted if the user accepts the completion.
	 *
	 * @return The suffix that should be inserted to complete the text,
	 * or {@code null} if the full text does not match the partial text in a way that allows for completion.
	 */
	@Nullable
	public String completionSuffix(@Nonnull String partialText, @Nonnull String fullText) {
		if (!fullText.startsWith(partialText)) {
			int space = partialText.indexOf(' ');
			if (space >= 0)
				return completionSuffix(partialText.substring(space + 1), fullText);
			int dot = partialText.indexOf('.');
			if (dot >= 0)
				return completionSuffix(partialText.substring(dot + 1), fullText);

			space = fullText.indexOf(' ');
			if (space >= 0)
				return completionSuffix(partialText, fullText.substring(space + 1));
			dot = fullText.indexOf('.');
			if (dot >= 0)
				return completionSuffix(partialText, fullText.substring(dot + 1));
			return null;
		}
		return fullText.substring(partialText.length());
	}

	/**
	 * Extracts member access or method reference context from the given line prefix.
	 *
	 * @param linePrefix
	 * 		The text from the start of the line, up to the caret position.
	 * @param lineStart
	 * 		The offset in the full text where the line starts.
	 *
	 * @return A {@link JavaLexicalContext} representing the member access or method reference context,
	 * or {@link JavaLexicalContext#none()} if no such context could be determined.
	 */
	@Nonnull
	private static JavaLexicalContext extractMemberContext(@Nonnull String linePrefix, int lineStart) {
		int partialStart = linePrefix.length();
		while (partialStart > 0 && isIdentifierChar(linePrefix.charAt(partialStart - 1)))
			partialStart--;
		String partial = linePrefix.substring(partialStart);
		JavaLexicalContext methodRefContext = buildReceiverContext(linePrefix, lineStart, partial, ContextKind.METHOD_REFERENCE, partialStart - 2, partialStart);
		if (methodRefContext.kind() != ContextKind.NONE)
			return methodRefContext;
		JavaLexicalContext memberContext = buildReceiverContext(linePrefix, lineStart, partial, ContextKind.MEMBER, partialStart - 1, partialStart);
		if (memberContext.kind() != ContextKind.NONE)
			return memberContext;
		return JavaLexicalContext.none();
	}

	/**
	 * Builds a receiver context for member access or method reference based on the given line prefix and separator positions.
	 *
	 * @param linePrefix
	 * 		The text from the start of the line, up to the caret position.
	 * @param lineStart
	 * 		The offset in the full text where the line starts.
	 * @param partial
	 * 		The partial text that the user has typed for the member or method reference.
	 * @param kind
	 * 		The kind of context to build <i>(member access or method reference)</i>.
	 * @param separatorStart
	 * 		The start index of the separator <i>({@code '.'} for member access, {@code '::'} for method reference)</i> in the line prefix.
	 * @param separatorEnd
	 * 		The end index of the separator in the line prefix.
	 *
	 * @return A {@link JavaLexicalContext} representing the member access or method reference context,
	 * or {@link JavaLexicalContext#none()} if no such context could be determined.
	 */
	@Nonnull
	private static JavaLexicalContext buildReceiverContext(@Nonnull String linePrefix, int lineStart,
	                                                       @Nonnull String partial, @Nonnull ContextKind kind,
	                                                       int separatorStart, int separatorEnd) {
		// Sanity check the separator position and text.
		if (separatorStart < 0 || separatorEnd > linePrefix.length())
			return JavaLexicalContext.none();

		// Verify the separator text matches the expected kind.
		String separator = kind == ContextKind.METHOD_REFERENCE ? "::" : ".";
		if (!linePrefix.substring(separatorStart, separatorEnd).equals(separator))
			return JavaLexicalContext.none();

		// Move the receiver end back to skip any whitespace before the separator.
		int receiverEnd = separatorStart - 1;
		while (receiverEnd >= 0 && Character.isWhitespace(linePrefix.charAt(receiverEnd)))
			receiverEnd--;
		if (receiverEnd < 0)
			return JavaLexicalContext.none();

		// Find the start of the receiver expression.
		int receiverStart = findReceiverStart(linePrefix, receiverEnd);
		if (receiverStart < 0)
			return JavaLexicalContext.none();

		// Extract the receiver text and trim it. If it's empty, we have no meaningful context.
		String receiverText = linePrefix.substring(receiverStart, receiverEnd + 1).trim();
		if (receiverText.isEmpty())
			return JavaLexicalContext.none();
		return new JavaLexicalContext(kind, partial, receiverText, lineStart + receiverEnd, false, KeywordSite.UNKNOWN);
	}

	/**
	 * Infers the keyword site based on the structure of the code before the caret and the partial text being completed.
	 *
	 * @param text
	 * 		The full text of the Java source.
	 * @param caret
	 * 		The caret position within the text to analyze.
	 * @param partial
	 * 		The partial text that the user has typed for the current identifier.
	 *
	 * @return The inferred {@link KeywordSite} based on the code structure and partial text.
	 */
	@Nonnull
	private static KeywordSite inferKeywordSite(@Nonnull String text, int caret, @Nonnull String partial) {
		StructureState state = analyzeStructure(text, caret);
		String beforeIdentifier = state.codePrefix().substring(0, Math.max(0, state.codePrefix().length() - partial.length()));
		String clause = trailingClause(beforeIdentifier);

		// Check if we're inside a method body.
		// If we can be specific we'll look for expression keywords, otherwise we'll just say it's a method body.
		if (state.currentBlock() == BlockKind.METHOD_BODY)
			return looksExpressionLike(clause) ? KeywordSite.EXPRESSION_LIKE : KeywordSite.METHOD_BODY;

		// Check if we're in the 'class X extends Y' part of a type header,
		// which can look like a type body declaration until you see the full clause.
		if (looksTypeHeader(clause))
			return KeywordSite.TYPE_HEADER;

		// Check if we're in a type body, which can be a lot of different things,
		// but if we see method-like syntax we'll assume it's a method header, otherwise it's just a type body declaration.
		if (state.currentBlock() == BlockKind.TYPE) {
			if (looksMethodHeader(clause))
				return KeywordSite.METHOD_HEADER;
			if (looksExpressionLike(clause))
				return KeywordSite.EXPRESSION_LIKE;
			return KeywordSite.TYPE_BODY_DECLARATION;
		}

		// If we see expression-like syntax at the top level, we'll assume it's an expression context, otherwise it's a top-level declaration.
		if (looksExpressionLike(clause))
			return KeywordSite.EXPRESSION_LIKE;
		return KeywordSite.TOP_LEVEL_DECLARATION;
	}

	/**
	 * @param text
	 * 		The full text of the Java source.
	 * @param caret
	 * 		The caret position within the text to analyze.
	 *
	 * @return A {@link StructureState} containing the code prefix up to the caret with non-code elements stripped out
	 * and the kind of block we're currently in based on the braces we've seen.
	 */
	@Nonnull
	private static StructureState analyzeStructure(@Nonnull String text, int caret) {
		SourceState state = SourceState.CODE;
		boolean escaped = false;
		Deque<BlockKind> blockStack = new ArrayDeque<>();
		StringBuilder codePrefix = new StringBuilder(caret);

		for (int i = 0; i < caret; i++) {
			char c = text.charAt(i);
			switch (state) {
				case CODE -> {
					if (c == '/' && i + 1 < caret) {
						char next = text.charAt(i + 1);
						if (next == '/') {
							state = SourceState.LINE_COMMENT;
							codePrefix.append("  ");
							i++;
							continue;
						} else if (next == '*') {
							state = SourceState.BLOCK_COMMENT;
							codePrefix.append("  ");
							i++;
							continue;
						}
					} else if (c == '"') {
						state = SourceState.STRING;
						codePrefix.append(' ');
						continue;
					} else if (c == '\'') {
						state = SourceState.CHARACTER;
						codePrefix.append(' ');
						continue;
					}

					if (c == '{') {
						blockStack.push(classifyBlock(codePrefix));
						codePrefix.append(c);
					} else if (c == '}') {
						if (!blockStack.isEmpty())
							blockStack.pop();
						codePrefix.append(c);
					} else {
						codePrefix.append(c);
					}
				}
				case LINE_COMMENT -> {
					codePrefix.append(' ');
					if (c == '\n')
						state = SourceState.CODE;
				}
				case BLOCK_COMMENT -> {
					codePrefix.append(' ');
					if (c == '*' && i + 1 < caret && text.charAt(i + 1) == '/') {
						codePrefix.append(' ');
						state = SourceState.CODE;
						i++;
					}
				}
				case STRING -> {
					codePrefix.append(' ');
					if (escaped) {
						escaped = false;
					} else if (c == '\\') {
						escaped = true;
					} else if (c == '"') {
						state = SourceState.CODE;
					}
				}
				case CHARACTER -> {
					codePrefix.append(' ');
					if (escaped) {
						escaped = false;
					} else if (c == '\\') {
						escaped = true;
					} else if (c == '\'') {
						state = SourceState.CODE;
					}
				}
			}
		}

		BlockKind currentBlock = blockStack.isEmpty() ? BlockKind.NONE : blockStack.peek();
		return new StructureState(codePrefix.toString(), currentBlock);
	}

	/**
	 * @param codePrefix
	 * 		The code prefix up to the current block, with non-code elements stripped out.
	 *
	 * @return The kind of block we're currently in based on the braces we've seen and the code structure before it.
	 */
	@Nonnull
	private static BlockKind classifyBlock(@Nonnull CharSequence codePrefix) {
		String clause = trailingClause(codePrefix.toString());
		if (looksTypeHeader(clause))
			return BlockKind.TYPE;
		if (EXECUTABLE_BLOCK_PATTERN.matcher(normalizeWhitespace(clause)).matches())
			return BlockKind.METHOD_BODY;
		return BlockKind.UNKNOWN;
	}

	/**
	 * @param text
	 * 		Text to extract the trailing clause from.
	 *
	 * @return The text after the last semicolon or brace, which can help determine the current code context.
	 */
	@Nonnull
	private static String trailingClause(@Nonnull String text) {
		int delimiter = Math.max(
				Math.max(text.lastIndexOf(';'), text.lastIndexOf('{')),
				text.lastIndexOf('}')
		);
		return text.substring(Math.max(0, delimiter + 1));
	}

	/**
	 * @param clause
	 * 		Partial clause to analyze.
	 *
	 * @return {@code true} when it looks like we're in the header of a type declaration.
	 */
	private static boolean looksTypeHeader(@Nonnull String clause) {
		String normalized = normalizeWhitespace(clause);
		if (TYPE_HEADER_PATTERN.matcher(normalized).matches())
			return true;
		return RegexUtil.getMatcher("\\b(class|interface|enum|record)\\b[^;{}]*$", normalized).matches();
	}

	/**
	 * @param clause
	 * 		Partial clause to analyze.
	 *
	 * @return {@code true} when it looks like we're in the header of a method declaration.
	 */
	private static boolean looksMethodHeader(@Nonnull String clause) {
		return METHOD_HEADER_PATTERN.matcher(normalizeWhitespace(clause)).matches();
	}

	/**
	 * @param clause
	 * 		Partial clause to analyze.
	 *
	 * @return {@code true} when it looks like we're in an expression context.
	 */
	private static boolean looksExpressionLike(@Nonnull String clause) {
		return EXPRESSION_LIKE_PATTERN.matcher(normalizeWhitespace(clause)).matches();
	}

	/**
	 * @param text
	 * 		Text to normalize whitespace in.
	 *
	 * @return Text trimmed and whitespaces collapsed to single spaces.
	 */
	@Nonnull
	private static String normalizeWhitespace(@Nonnull String text) {
		return text.trim().replaceAll("\\s+", " ");
	}

	/**
	 * Extracts the start index of the receiver expression for member access or method reference based on the character at the given end index.
	 *
	 * @param text
	 * 		The full text of the line prefix.
	 * @param end
	 * 		The index of the last character of the receiver expression in the line prefix.
	 *
	 * @return The start index of the receiver expression, or {@code -1} if no valid receiver expression could be found.
	 */
	private static int findReceiverStart(@Nonnull String text, int end) {
		char c = text.charAt(end);
		if (c == '"' || c == '\'')
			return findQuotedLiteralStart(text, end, c);
		if (Character.isJavaIdentifierPart(c) || c == '$')
			return findQualifiedIdentifierStart(text, end);
		if (c == ')' || c == ']')
			return findBalancedExpressionStart(text, end);
		return -1;
	}

	/**
	 * Extracts the start index of a qualified identifier <i>(which may include dots)</i> in the given text,
	 * starting from the given end index and moving backwards.
	 *
	 * @param text
	 * 		The full text of the line prefix.
	 * @param end
	 * 		The index of the last character of the qualified identifier in the line prefix.
	 *
	 * @return The start index of the qualified identifier, or {@code -1} if no valid qualified identifier could be found.
	 */
	private static int findQualifiedIdentifierStart(@Nonnull String text, int end) {
		int start = end;
		while (start >= 0) {
			char c = text.charAt(start);
			if (Character.isJavaIdentifierPart(c) || c == '$' || c == '.')
				start--;
			else
				break;
		}
		return start + 1;
	}

	/**
	 * Extracts the start index of a quoted literal <i>(string or character)</i> in the given text,
	 * starting from the given end index and moving backwards, while properly handling escaped quotes.
	 *
	 * @param text
	 * 		The full text of the line prefix.
	 * @param end
	 * 		The index of the last character of the quoted literal in the line prefix.
	 * @param quote
	 * 		The quote character that delimits the literal <i>(either {@code '"'} for strings or {@code '\''} for characters)</i>.
	 *
	 * @return The start index of the quoted literal, or {@code -1} if no valid quoted literal could be found.
	 */
	private static int findQuotedLiteralStart(@Nonnull String text, int end, char quote) {
		boolean escaped = false;
		for (int i = end - 1; i >= 0; i--) {
			char c = text.charAt(i);
			if (escaped) {
				escaped = false;
				continue;
			}
			if (c == '\\') {
				escaped = true;
				continue;
			}
			if (c == quote)
				return i;
		}
		return -1;
	}

	/**
	 * Extracts the start index of a balanced expression <i>(parentheses or brackets)</i> in the given text,
	 * starting from the given end index and moving backwards, while properly handling nested expressions.
	 *
	 * @param text
	 * 		The full text of the line prefix.
	 * @param end
	 * 		The index of the last character of the balanced expression in the line prefix,
	 * 		which should be either a closing parenthesis {@code ')'} or a closing bracket {@code ']'}.
	 *
	 * @return The start index of the balanced expression, or {@code -1} if no valid balanced expression could be found.
	 */
	private static int findBalancedExpressionStart(@Nonnull String text, int end) {
		int balance = 0;
		char close = text.charAt(end);
		char open = close == ')' ? '(' : '[';
		for (int i = end; i >= 0; i--) {
			char c = text.charAt(i);
			if (c == close) {
				balance++;
			} else if (c == open) {
				balance--;
				if (balance == 0)
					return i;
			}
		}
		return -1;
	}

	/**
	 * @param prefix
	 * 		The text before the caret, up to the start of the current identifier.
	 *
	 * @return {@code true} if the prefix indicates we are in an annotation context <i>(Like if we just typed '@')</i>, {@code false} otherwise.
	 */
	private static boolean isAnnotationContext(@Nonnull String prefix) {
		return prefix.stripTrailing().endsWith("@");
	}

	/**
	 * @param prefix
	 * 		The text before the caret, up to the start of the current identifier.
	 *
	 * @return {@code true} if the prefix indicates we are in a type context
	 * <i>(Like if we just typed {@code new} or {@code extends}, etc.)</i>, {@code false} otherwise.
	 */
	private static boolean isTypeContext(@Nonnull String prefix) {
		String stripped = prefix.stripTrailing();
		if (stripped.endsWith("new")
				|| stripped.endsWith("extends")
				|| stripped.endsWith("implements")
				|| stripped.endsWith("throws"))
			return true;
		return stripped.matches(".*\\b(implements|extends|throws)\\s+[^;]*,");
	}

	/**
	 * @param text
	 * 		The text before the caret, up to the start of the current identifier.
	 *
	 * @return The trailing identifier from the given text.
	 */
	@Nonnull
	private static String trailingIdentifier(@Nonnull String text) {
		int start = text.length();
		while (start > 0 && isIdentifierChar(text.charAt(start - 1)))
			start--;
		return text.substring(start);
	}

	/**
	 * @param text
	 * 		The text before the caret, up to the start of the current identifier.
	 *
	 * @return The trailing qualified name <i>(which may include dots)</i> from the given text.
	 */
	@Nonnull
	private static String trailingQualifiedName(@Nonnull String text) {
		int start = text.length();
		while (start > 0) {
			char c = text.charAt(start - 1);
			if (isIdentifierChar(c) || c == '.')
				start--;
			else
				break;
		}
		return text.substring(start);
	}

	/**
	 * @param c
	 * 		The character to check.
	 *
	 * @return {@code true} if the given character is a valid part of a Java identifier <i>(including '$')</i>, {@code false} otherwise.
	 */
	private static boolean isIdentifierChar(char c) {
		return Character.isJavaIdentifierPart(c) || c == '$';
	}

	/**
	 * Determines the lexical state of the source code at the given caret position within the provided Java source text.
	 *
	 * @param text
	 * 		Full text of the Java source.
	 * @param caret
	 * 		Caret position within the text to analyze.
	 *
	 * @return The {@link SourceState} representing the lexical state at the caret position.
	 */
	@Nonnull
	private static SourceState sourceStateAt(@Nonnull String text, int caret) {
		SourceState state = SourceState.CODE;
		boolean escaped = false;

		for (int i = 0; i < caret; i++) {
			char c = text.charAt(i);
			switch (state) {
				case CODE -> {
					if (c == '/' && i + 1 < caret) {
						char next = text.charAt(i + 1);
						if (next == '/') {
							state = SourceState.LINE_COMMENT;
							i++;
						} else if (next == '*') {
							state = SourceState.BLOCK_COMMENT;
							i++;
						}
					} else if (c == '"') {
						state = SourceState.STRING;
					} else if (c == '\'') {
						state = SourceState.CHARACTER;
					}
				}
				case LINE_COMMENT -> {
					if (c == '\n')
						state = SourceState.CODE;
				}
				case BLOCK_COMMENT -> {
					if (c == '*' && i + 1 < caret && text.charAt(i + 1) == '/') {
						state = SourceState.CODE;
						i++;
					}
				}
				case STRING -> {
					if (escaped) {
						escaped = false;
					} else if (c == '\\') {
						escaped = true;
					} else if (c == '"') {
						state = SourceState.CODE;
					}
				}
				case CHARACTER -> {
					if (escaped) {
						escaped = false;
					} else if (c == '\\') {
						escaped = true;
					} else if (c == '\'') {
						state = SourceState.CODE;
					}
				}
			}
		}
		return state;
	}

	private enum BlockKind {
		NONE,
		TYPE,
		METHOD_BODY,
		UNKNOWN
	}

	private record StructureState(@Nonnull String codePrefix, @Nonnull BlockKind currentBlock) {}
}
