package software.coley.recaf.services.decompile.filter;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.util.EscapeUtil;
import software.coley.recaf.workspace.model.Workspace;

public class UnicodeUnescapeOutputTextFilter implements OutputTextFilter {
	@Nonnull
	@Override
	public String filter(@Nonnull Workspace workspace, @Nonnull ClassInfo classInfo, @Nonnull String code) {
		return EscapeUtil.unescapeUnicodeIf(code, UnicodeUnescapeOutputTextFilter::isSafeDisplayCodePoint);
	}

	static boolean isSafeDisplayCodePoint(int codePoint) {
		if (codePoint <= 0 || codePoint == EscapeUtil.TERMINATOR || codePoint > Character.MAX_VALUE)
			return false;
		if (EscapeUtil.isWhitespaceChar((char) codePoint))
			return false;
		return switch (Character.getType((char) codePoint)) {
			case Character.UPPERCASE_LETTER, Character.LOWERCASE_LETTER, Character.TITLECASE_LETTER,
					Character.MODIFIER_LETTER, Character.OTHER_LETTER, Character.DECIMAL_DIGIT_NUMBER,
					Character.LETTER_NUMBER, Character.OTHER_NUMBER, Character.DASH_PUNCTUATION,
					Character.START_PUNCTUATION, Character.END_PUNCTUATION, Character.INITIAL_QUOTE_PUNCTUATION,
					Character.FINAL_QUOTE_PUNCTUATION, Character.OTHER_PUNCTUATION, Character.MATH_SYMBOL,
					Character.CURRENCY_SYMBOL, Character.MODIFIER_SYMBOL, Character.OTHER_SYMBOL -> true;
			default -> false;
		};
	}
}
