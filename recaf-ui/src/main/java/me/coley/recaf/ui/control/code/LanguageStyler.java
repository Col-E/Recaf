package me.coley.recaf.ui.control.code;

import jregex.Matcher;
import jregex.Pattern;
import me.coley.recaf.util.RegexUtil;
import me.coley.recaf.util.Threads;
import me.coley.recaf.util.logging.Logging;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Utility for applying a given theme to some text based on the given language rule-set.
 *
 * @author Matt Coley
 */
public class LanguageStyler {
	private static final Logger logger = Logging.get(LanguageStyler.class);
	private static final String DEFAULT_CLASS = "text";
	private final SyntaxArea editor;
	private final Language language;

	/**
	 * @param editor
	 * 		The editor context.
	 * @param language
	 * 		Language with rules to apply to text.
	 */
	public LanguageStyler(SyntaxArea editor, Language language) {
		if (editor == null)
			throw new IllegalStateException("SyntaxArea must not be null");
		if (language == null)
			throw new IllegalStateException("Language must not be null");
		this.editor = editor;
		this.language = language;
	}

	/**
	 * Compute and apply all language pattern matches in the {@link #editor}'s document text for the given range.
	 * In some cases for validity purposes style ranges may stretch past the start/end positions.
	 * <br>
	 * For example, if a user inserts {@code '//'} before a line,
	 * the entire line style would be restyled as a single line comment even though only two characters were changed.
	 * <br>
	 * And for the start range changing, a user could delete a {@code "} from a string,
	 * which would require restyling the entire length of the string, and not just the character modified.
	 *
	 * @param start
	 * 		Start position in document where edits occurred.
	 * @param end
	 * 		End position in the document where edits occurred.
	 */
	public void styleRange(int start, int end) {
		String text = editor.getText();
		// Fit range based on rule matching needs
		end = expandEndForwards(text, end);
		start = expandStartBackwards(text, start, end);
		// Update style in updated range
		styleAtWithRange(start, end - start);
	}

	private int expandStartBackwards(String text, int start, int end) {
		// Validate inputs
		if (start <= 0) {
			return 0;
		}
		// Sanitize into document text range
		start = Math.min(start, text.length() - 1);
		// Ensure the start position begins in a non-styled area, preferably at the start of an empty line.
		while (start > 0) {
			if (text.charAt(start) == '\n') {
				break;
			}
			start--;
		}
		while (start > 0) {
			Collection<String> styles = editor.getStyleAtPosition(start);
			if (styles.isEmpty() || (styles.size() == 1 && styles.iterator().next().equals(DEFAULT_CLASS))) {
				break;
			}
			start--;
		}
		// Handle update for backtracking
		// - Moves the start position to what a point where the beginning of the rule should match.
		String textRange = text.substring(start, end);
		for (LanguageRule rule : editor.getLanguage().getRules()) {
			// Only move if the text contains backtrack trigger
			if (rule.requireBacktracking() && textRange.contains(rule.getBacktrackTrigger())) {
				String stopText = rule.getBacktrackStop();
				// Expand start range until we contain the backtrack stop pattern.
				int tempStart = start;
				while (tempStart > 0 && !textRange.contains(stopText)) {
					textRange = text.substring(tempStart, end);
					tempStart -= stopText.length();
				}
				// If the expanded range has the stop pattern, update the start position.
				// If it was not found then we don't want the regex handling later to start from 0
				// when there is no patterns to match that far back for the current range.
				if (textRange.contains(stopText)) {
					start = tempStart;
				}
			}
		}
		return start;
	}

	private int expandEndForwards(String text, int end) {
		// Sanitize into document text range
		end = Math.min(end, text.length());
		// Ensure the end position begins in a non-styled area, preferably at the end of a line.
		while (end < text.length() - 1) {
			if (text.charAt(end + 1) == '\n') {
				break;
			}
			end++;
		}
		while (end < text.length()) {
			Collection<String> styles = editor.getStyleAtPosition(end);
			if (styles.isEmpty() || (styles.size() == 1 && styles.iterator().next().equals(DEFAULT_CLASS))) {
				break;
			}
			end++;
		}
		return end;
	}

	/**
	 * Compute and apply all language pattern matches in the {@link #editor}'s document text.
	 */
	public void styleCompleteDocument() {
		styleAtWithRange(0, Integer.MAX_VALUE);
	}

	private void styleAtWithRange(int start, int matcherRange) {
		String text = editor.getText();
		if (start > 0) {
			// Only use the text from the start position onwards
			text = text.substring(start);
		}
		Matcher matcher = getPattern().matcher(text);
		int lastKwEnd = 0;
		StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
		boolean modified = false;
		try {
			while (matcher.find()) {
				if (Thread.interrupted())
					return;
				String styleClass = getClassFromGroup(matcher);
				if (styleClass == null) {
					logger.warn("Could not find matching class in language '{}' for match '{}'",
							language.getName(), matcher.target());
					styleClass = DEFAULT_CLASS;
				}
				// Create a span for the unmatched range from the prior match
				spansBuilder.add(Collections.singleton(DEFAULT_CLASS), matcher.start() - lastKwEnd);
				// Create a span for the matched range
				spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
				lastKwEnd = matcher.end();
				modified = true;
				// Stop searching after the end range
				if (lastKwEnd >= matcherRange) {
					break;
				}
			}
			// Remaining text gets the default class
			int end = Math.min(matcherRange - lastKwEnd, text.length() - lastKwEnd);
			if (end > 0) {
				modified = true;
				spansBuilder.add(Collections.singleton(DEFAULT_CLASS), end);
			}
		} catch (NullPointerException npe) {
			// There was once some odd behavior in 'matcher.find()' which caused NPE...
			// This seems to have been fixed, but we will check for regressions
			logger.error("Error occurred when computing styles:", npe);
		}
		if (modified) {
			StyleSpans<Collection<String>> spans = spansBuilder.create();
			// Update editor at position
			Threads.runFx(() -> editor.setStyleSpans(start, spans));
		}
	}

	/**
	 * @return Compiled regex pattern from {@link #getRules() all existing rules}.
	 */
	public Pattern getPattern() {
		if (getRules().isEmpty())
			return RegexUtil.pattern("({EMPTY}EMPTY)");
		StringBuilder sb = new StringBuilder();
		for (LanguageRule rule : getRules())
			sb.append("({" + rule.getPatternGroupName() + "}" + rule.getPattern() + ")|");
		return RegexUtil.pattern(sb.substring(0, sb.length() - 1));
	}

	/**
	 * @return List of language rules.
	 */
	private List<LanguageRule> getRules() {
		return language.getRules();
	}

	/**
	 * Fetch the CSS class name to use based on the matched group.
	 *
	 * @param matcher
	 * 		Matcher that has found a group.
	 *
	 * @return CSS class name <i>(Raw name of regex rule)</i>
	 */
	private String getClassFromGroup(Matcher matcher) {
		for (LanguageRule rule : getRules())
			if (matcher.group(rule.getPatternGroupName()) != null)
				return rule.getName();
		return null;
	}
}
