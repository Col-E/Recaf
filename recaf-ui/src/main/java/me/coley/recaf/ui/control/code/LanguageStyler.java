package me.coley.recaf.ui.control.code;

import jregex.Matcher;
import jregex.Pattern;
import me.coley.recaf.util.RegexUtil;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.threading.ThreadUtil;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Utility for applying a given theme to some text based on the given language rule-set.
 *
 * @author Matt Coley
 * @see Styleable Component to style text of.
 */
public class LanguageStyler {
	private static final Logger logger = Logging.get(LanguageStyler.class);
	private static final Pattern EMPTY_PATTERN = RegexUtil.pattern("({EMPTY}EMPTY)");
	private static final int MAX_MATCH_LOG_SIZE = 20;
	private static final String DEFAULT_CLASS = "text";
	private final Styleable handler;
	private Language language;

	/**
	 * @param language
	 * 		Language to be used for stylization.
	 */
	public LanguageStyler(Language language, Styleable handler) {
		this.language = language;
		this.handler = handler;
	}

	/**
	 * @return Target language.
	 */
	public Language getLanguage() {
		return language;
	}

	/**
	 * @param language
	 * 		Target language.
	 */
	public void setLanguage(Language language) {
		this.language = language;
	}

	/**
	 * Compute and apply all language pattern matches in the document text.
	 *
	 * @param text
	 * 		Complete document text.
	 *
	 * @return Future for tracking completion of style computations. Delegated to {@link Styleable#onClearStyle()}
	 * or {@link Styleable#onApplyStyle(int, List)}.
	 */
	public CompletableFuture<Void> styleCompleteDocument(String text) {
		return styleRange(text, 0, Integer.MAX_VALUE);
	}

	/**
	 * Compute and apply all language pattern matches in the document text for the given range.
	 * In some cases for validity purposes style ranges may stretch past the start/end positions.
	 * <br>
	 * For example, if a user inserts {@code '//'} before a line,
	 * the entire line style would be restyled as a single line comment even though only two characters were changed.
	 * <br>
	 * And for the start range changing, a user could delete a {@code "} from a string,
	 * which would require restyling the entire length of the string, and not just the character modified.
	 *
	 * @param styleable
	 * 		Style lookup for the given text.
	 * @param text
	 * 		The text to style.
	 * @param start
	 * 		Start position in document where edits occurred.
	 * @param end
	 * 		End position in the document where edits occurred.
	 *
	 * @return Future for tracking completion of style computations. Delegated to {@link Styleable#onClearStyle()}
	 * or {@link Styleable#onApplyStyle(int, List)}.
	 */
	public CompletableFuture<Void> styleFlexibleRange(Styleable styleable, String text, int start, int end) {
		// Fit range based on rule matching needs
		end = expandEndForwards(styleable, text, end);
		start = expandStartBackwards(styleable, text, start, end);
		// Update style in updated range
		return styleRange(text, start, end - start);
	}

	private int expandStartBackwards(Styleable styleable, String text, int start, int end) {
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
			Collection<String> styles = styleable.getStyleAtPosition(start);
			if (styles.isEmpty() || (styles.size() == 1 && styles.iterator().next().equals(DEFAULT_CLASS))) {
				break;
			}
			start--;
		}
		// Handle update for backtracking
		// - Moves the start position to what a point where the beginning of the rule should match.
		String textRange = text.substring(start, end);
		for (LanguageRule rule : language.getRules()) {
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

	private int expandEndForwards(Styleable styleable, String text, int end) {
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
			Collection<String> styles = styleable.getStyleAtPosition(end);
			if (styles.isEmpty() || (styles.size() == 1 && styles.iterator().next().equals(DEFAULT_CLASS))) {
				break;
			}
			end++;
		}
		return end;
	}

	/**
	 * @param text
	 * 		Complete document text.
	 * @param start
	 * 		Start range.
	 * @param matcherRange
	 * 		Range size.
	 *
	 * @return Future for tracking completion of style computations. Delegated to {@link Styleable#onClearStyle()}
	 * or {@link Styleable#onApplyStyle(int, List)}.
	 */
	public CompletableFuture<Void> styleRange(String text, int start, int matcherRange) {
		if (start > 0) {
			// Only use the text from the start position onwards
			text = text.substring(start);
		}
		Pattern pattern = getPattern();
		if (pattern == null || pattern == EMPTY_PATTERN) {
			return handler.onClearStyle();
		}
		Matcher matcher = pattern.matcher(text);
		int lastKwEnd = 0;
		List<Section> sections = new ArrayList<>();
		boolean modified = false;
		try {
			while (matcher.find()) {
				if (Thread.interrupted())
					return ThreadUtil.failedFuture(new InterruptedException());
				String styleClass = getClassFromGroup(matcher);
				String target = matcher.group(0);
				if (styleClass == null) {
					if (target.length() > MAX_MATCH_LOG_SIZE) {
						target = target.substring(0, MAX_MATCH_LOG_SIZE) + "...";
					}
					logger.warn("Could not find matching class in language '{}' for match '{}'",
							language.getName(), target);
					styleClass = DEFAULT_CLASS;
				}
				// Create a span for the unmatched range from the prior match
				String unmatched = text.substring(lastKwEnd, matcher.start());
				if (!unmatched.isEmpty())
					sections.add(new Section(Collections.singleton(DEFAULT_CLASS), matcher.start() - lastKwEnd, unmatched));
				// Create a span for the matched range
				sections.add(new Section(Collections.singleton(styleClass), matcher.end() - matcher.start(), target));
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
				sections.add(new Section(Collections.singleton(DEFAULT_CLASS), end, text.substring(lastKwEnd)));
			}
		} catch (NullPointerException npe) {
			// There was once some odd behavior in 'matcher.find()' which caused NPE...
			// This seems to have been fixed, but we will check for regressions
			logger.error("Error occurred when computing styles:", npe);
		}
		if (modified) {
			return handler.onApplyStyle(start, sections);
		}
		return CompletableFuture.completedFuture(null);
	}


	/**
	 * @return Compiled regex pattern from {@link #getRules() all existing rules}.
	 */
	private Pattern getPattern() {
		if (getRules().isEmpty())
			return EMPTY_PATTERN;
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

	/**
	 * Describes a section of styled text.
	 */
	public static class Section {
		/**
		 * Style rules for the section.
		 */
		public final Collection<String> classes;
		/**
		 * Length of region, short for  {@link #text}'s length.
		 */
		public final int length;
		/**
		 * Text of the section.
		 */
		public final String text;

		private Section(Collection<String> classes, int length, String text) {
			this.classes = classes;
			this.length = length;
			this.text = text;
		}

		@Override
		public String toString() {
			return "Section{" +
					"classes=" + classes +
					", text='" + text + '\'' +
					'}';
		}
	}
}
