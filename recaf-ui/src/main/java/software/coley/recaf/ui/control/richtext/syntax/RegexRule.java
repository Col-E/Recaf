package software.coley.recaf.ui.control.richtext.syntax;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Regex rule data type.
 *
 * @param name
 * 		Name of rule. Must not contain any whitespaces or special characters.
 * @param regex
 * 		Regex pattern.
 * @param classes
 * 		Style classes to apply to matched ranges within a {@link SyntaxHighlighter}.
 * @param subRules
 * 		Sub-rules which can be used to create sub-matches within ranges matched by this rule.
 * @param backtrackMark
 * 		Used for rules that are variable length. Indicates the start text of such matches.
 * 		See {@link RegexSyntaxHighlighter#expandRange(String, int, int)} for usage.
 * @param advanceMark
 * 		Used for rules that are variable length. Indicates the end text of such matches.
 * 		See {@link RegexSyntaxHighlighter#expandRange(String, int, int)} for usage.
 *
 * @author Matt Coley
 * @see RegexSyntaxHighlighter
 */
public record RegexRule(@JsonProperty("name") String name,
						@JsonProperty("regex") String regex,
						@JsonProperty("classes") List<String> classes,
						@JsonProperty("sub-rules") List<RegexRule> subRules,
						@JsonProperty("backtrack-mark") String backtrackMark,
						@JsonProperty("advance-mark") String advanceMark) {
}
