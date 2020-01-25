package me.coley.recaf.ui.controls.text.model;

import jregex.Matcher;
import jregex.Pattern;
import me.coley.recaf.util.Log;
import me.coley.recaf.util.RegexUtil;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.*;

/**
 * Utility for applying a given theme to some text based on the given language rule-set.
 *
 * @author Matt
 */
public class LanguageStyler {
	private Language language;

	/**
	 * @param language
	 * 		Language with rules to apply to text.
	 */
	public LanguageStyler(Language language) {
		if(language == null)
			throw new IllegalStateException("Language must not be null");
		this.language = language;
	}

	/**
	 * @param text
	 * 		Text to apply styles to.
	 *
	 * @return Stylized regions of the text <i>(via css tags)</i>.
	 */
	public StyleSpans<Collection<String>> computeStyle(String text) {
		Matcher matcher = getPattern().matcher(text);
		int lastKwEnd = 0;
		StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
		try {
			while(matcher.find()) {
				String styleClass = getClassFromGroup(matcher);
				if(styleClass == null)
					styleClass = "text";
				spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
				spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
				lastKwEnd = matcher.end();
			}
		} catch(NullPointerException npe) {
			// There was once some odd behavior in 'matcher.find()' which caused NPE...
			// This seems to have been fixed, but we will check for regressions
			Log.error(npe, "Error occurred when computing styles:");
		}
		spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
		return spansBuilder.create();
	}

	/**
	 * @return Compiled regex pattern from {@link #getRules() all existing rules}.
	 */
	public Pattern getPattern() {
		if(getRules().isEmpty())
			return RegexUtil.pattern("({EMPTY}EMPTY)");
		StringBuilder sb = new StringBuilder();
		for(Rule rule : getRules())
			sb.append("({" + rule.getPatternGroupName() + "}" + rule.getPattern() + ")|");
		return RegexUtil.pattern(sb.substring(0, sb.length() - 1));
	}

	/**
	 * @return List of language rules.
	 */
	private List<Rule> getRules() {
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
		for(Rule rule : getRules())
			if(matcher.group(rule.getPatternGroupName()) != null)
				return rule.getName();
		return null;
	}
}
