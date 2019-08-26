package me.coley.recaf.parse.assembly.parsers;

import jregex.Matcher;
import me.coley.recaf.parse.assembly.LineParseException;
import me.coley.recaf.parse.assembly.Parser;
import me.coley.recaf.util.OpcodeUtil;
import me.coley.recaf.util.RegexUtil;
import org.objectweb.asm.Handle;

import java.util.*;

import static org.objectweb.asm.Opcodes.H_INVOKEINTERFACE;

/**
 * Handle parser.
 *
 * @author Matt
 */
public class HandleParser extends Parser {
	private static final String HANDLE_PATTERN = "(?<=\\[)\\s*({TAG}\\S+)\\s*({OWNER}\\S+)\\s*" +
			"({NAME}\\S+)\\s*({DESC}\\S+)\\s*(?=\\])";

	/**
	 * Construct a handle parser.
	 *
	 * @param id
	 * 		Parser identifier.
	 */
	public HandleParser(String id) {
		super(id);
	}

	@Override
	public Object parse(String text) throws LineParseException {
		Matcher matcher = match(text);
		int tag = OpcodeUtil.nameToTag(matcher.group("TAG"));
		String owner = matcher.group("OWNER");
		String name = matcher.group("NAME");
		String desc = matcher.group("DESC");
		boolean itf = tag == H_INVOKEINTERFACE;
		return new Handle(tag, owner, name, desc, itf);
	}

	@Override
	public int endIndex(String text) throws LineParseException {
		return match(text).end() + 1;
	}

	@Override
	public List<String> getSuggestions(String text) throws LineParseException {
		return Collections.emptyList();
	}

	private Matcher match(String text) throws LineParseException {
		Matcher m = RegexUtil.getMatcher(HANDLE_PATTERN, text);
		if (m == null || !m.find())
			throw new LineParseException(text, "No handle to match");
		return m;
	}
}
