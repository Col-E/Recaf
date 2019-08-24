package me.coley.recaf.parse.assembly.parsers;

import me.coley.recaf.parse.assembly.LineParseException;
import me.coley.recaf.parse.assembly.Parser;
import me.coley.recaf.util.AutoCompleteUtil;
import me.coley.recaf.util.RegexUtil;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.function.Function;

/**
 * Field / method descriptor parser.
 *
 * @author Matt
 */
public class DescriptorParser extends Parser {
	private static final String FIELD_DESC_PATTERN = "[\\[\\/\\w\\$]+;?";
	private static final String METHOD_DESC_PATTERN = "\\([\\[\\/\\w\\$\\;]*\\)[\\[\\/\\w\\$]+;?";
	private final DescType type;

	/**
	 * Construct a name parser.
	 *
	 * @param type
	 * 		Type of descriptor being parsed.
	 * @param id
	 * 		Parser identifier.
	 **/
	public DescriptorParser(String id, DescType type) {
		super(id);
		this.type = type;
	}

	@Override
	public Object parse(String text) throws LineParseException {
		String token = getToken(text);
		// Verify type format
		try {
			if(type == DescType.METHOD)
				Type.getMethodType(token);
			else {
				Type t = Type.getType(token);
				while (t.getSort() == Type.ARRAY)
					t = t.getElementType();
				// Verify L...; pattern
				// - getDescriptor doesn't modify the original element type (vs getInternalName)
				String name = t.getDescriptor();
				if (name.startsWith("L") && !name.endsWith(";"))
					throw new LineParseException(text, "Invalid field type specified");
			}
		} catch(Exception ex) {
			throw new LineParseException(text, "Invalid type specified");
		}
		return token;
	}

	@Override
	public int endIndex(String text) throws LineParseException {
		String token = getToken(text);
		return text.indexOf(token) + token.length();
	}

	@Override
	public List<String> getSuggestions(String text) throws LineParseException {
		// Check if suggestions are even supported
		if (!type.isSupported())
			return Collections.emptyList();
		// Apply suggestions
		String token = getToken(text);
		return type.suggest(token);
	}

	private String getToken(String text) throws LineParseException {
		String token;
		if(type == DescType.METHOD)
			token = RegexUtil.getFirstToken(METHOD_DESC_PATTERN, text);
		else
			token = RegexUtil.getFirstToken(FIELD_DESC_PATTERN, text);
		if(token == null)
			throw new LineParseException(text, "No word to match");
		return token;
	}

	/**
	 * Kind of descriptor to parse.
	 *
	 * @author Matt
	 */
	public enum DescType {
		FIELD(true, AutoCompleteUtil::descriptorName),
		METHOD(false, s -> Collections.emptyList());

		private final boolean supported;
		private final Function<String, List<String>> completion;

		DescType(boolean supported, Function<String, List<String>> completion) {
			this.supported = supported;
			this.completion = completion;
		}

		boolean isSupported() {
			return supported;
		}

		List<String> suggest(String token) {
			return completion.apply(token);
		}
	}
}
