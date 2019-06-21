package me.coley.recaf.bytecode.search;

import java.util.*;
import java.util.regex.Pattern;

public class Parameter {
	private final List<String> skip = new ArrayList<>();
	private final List<Object> args;
	private final SearchType type;
	private StringMode stringMode = StringMode.CONTAINS;
	private boolean caseSensitive;

	/**
	 * Users are directed to use the static factory methods.
	 * 
	 * @param type
	 *            Search type.
	 * @param args
	 *            Search arguments.
	 */
	private Parameter(SearchType type, Object... args) {
		this.type = type;
		this.args = Arrays.asList(args);
	}

	/**
	 * List of skipped class-name prefixes. For example, to skip anything in
	 * the "me.example" package, just add "me.example" to the list.
	 * 
	 * @return Skipped class-name prefixes.
	 */
	public List<String> getSkipList() {
		return skip;
	}

	/**
	 * @return Search type.
	 */
	public SearchType getType() {
		return type;
	}

	/**
	 * @param type
	 * 		Other search type.
	 *
	 * @return {@code true} if matching types.
	 */
	public boolean isType(SearchType type) {
		return getType().equals(type);
	}

	/**
	 * @return How to use string arguments when looking for matching content
	 *         in method-code.
	 */
	public StringMode getStringMode() {
		return stringMode;
	}

	/**
	 * @param stringMode
	 *            How to use string arguments when looking for matching
	 *            content in method-code.
	 */
	public void setStringMode(StringMode stringMode) {
		this.stringMode = stringMode;
	}

	/**
	 * @return {@code true} if search is case-sensitive. Also {@code true}
	 *         if the {@link #getStringMode() string-mode} is regex, since
	 *         case-sensitivity is then overridden by it.
	 */
	public boolean isCaseSensitive() {
		return caseSensitive || getStringMode().equals(StringMode.REGEX);
	}

	/**
	 * @param caseSensitive
	 *            Searches to be case-sensitive when comparing string
	 *            values.
	 */
	public void setCaseSenstive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

	/**
	 * Get search argument.
	 * 
	 * @param index
	 *            Arg index.
	 * @return Arg value.
	 */
	@SuppressWarnings("unchecked")
	public <T> T getArg(int index) {
		return (T) args.get(index);
	}

	/**
	 * @param owner
	 *            Name of owner being checked.
	 * @param name
	 *            Member name.
	 * @param desc
	 *            Member descriptor.
	 * @return {@code true} if owner name matches the search argument for
	 *         owner + name + desc.
	 */
	public boolean validMember(String owner, String name, String desc) {
		if (getType() == SearchType.DECLARATION || getType() == SearchType.REFERENCE) {
			return check(0, owner, true) && check(1, name, true) && check(2, desc, true);
		}
		return false;
	}

	/**
	 * @param owner
	 *            Internal name of type.
	 * @return {@code true} if the internal name matches the search argument for owner.
	 */
	public boolean validType(String owner) {
		if (getType() == SearchType.DECLARATION || getType() == SearchType.REFERENCE) {
			return check(0, owner, false);
		}
		return false;
	}
	
	/**
	 * @param name
	 *            Internal name of type.
	 * @return {@code true} if the member name matches the search argument for name.
	 */
	public boolean validName(String name) {
		if (getType() == SearchType.DECLARATION || getType() == SearchType.REFERENCE) {
			return check(1, name, false);
		}
		return false;
	}
	
	/**
	 * @param name
	 *            Internal name of type.
	 * @return {@code true} if the internal desc matches the search argument for desc.
	 */
	public boolean validDesc(String desc) {
		if (getType() == SearchType.DECLARATION || getType() == SearchType.REFERENCE) {
			return check(2, desc, false);
		}
		return false;
	}

	/**
	 * Check if some input matches the content of some argument based on the
	 * current {@link #getStringMode() string-mode}.
	 * 
	 * @param arg
	 *            Argument to use.
	 * @param input
	 *            Content to check for match against arg content.
	 * @param fallback
	 *            Fallback return value if the arg value of the given index does
	 *            not exist.
	 * @return {@code true} if match.
	 */
	boolean check(int arg, String input, boolean fallback) {
		// bounds check
		if (arg >= args.size()) {
			return false;
		}
		// check if value exists
		Object argValue = getArg(arg);
		if (argValue == null) {
			return fallback;
		}
		// check for match
		String search = (String) argValue;
		// ensure proper case comparison.
		if (!isCaseSensitive()) {
			search = search.toLowerCase();
			input = input.toLowerCase();
		}
		switch (getStringMode()) {
		case CONTAINS:
			return input.contains(search);
		case ENDS_WITH:
			return input.endsWith(search);
		case EQUALITY:
			return input.equals(search);
		case REGEX:
			return Pattern.compile(search).matcher(input).find();
		case STARTS_WITH:
			return input.startsWith(search);
		default:
			return false;
		}
	}

	/**
	 * @return {@code true} if there is only one argument. Used for checking
	 *         for edge-cases usages of {@link #getType()}.
	 */
	public boolean singleArg() {
		// Cases where there is a single non-null argument.
		return this.args.stream().filter(o -> o != null).count() == 1;
	}
	
	@Override
	public String toString() { 
		StringBuilder sb = new StringBuilder();
		sb.append("Args[");
		for (Object o : args) {
			sb.append(o);
			sb.append(",");
		}
		sb.append("], Skip[");
		for (String s : skip) {
			sb.append(s);
			sb.append(",");
		}
		sb.append("], Type=" + type);
		sb.append(", StringMode=" + stringMode);
		sb.append(", Sensitive=" + caseSensitive);
		return sb.toString();
	}

	/**
	 * @param text
	 *            Text to search for.
	 * @return String search parameter.
	 */
	public static Parameter string(String text) {
		return new Parameter(SearchType.STRING, text);
	}

	/**
	 * @param value
	 *            Value to search for.
	 * @return Constant value search parameter.
	 */
	public static Parameter value(Number value) {
		return new Parameter(SearchType.VALUE, value);
	}

	/**
	 * Search for a sequence of opcodes.
	 * 
	 * For instance: <i>(Hello world opcodes)</i>
	 * 
	 * <pre>
	 * "GETSTATIC", "LDC", "INVOKEVIRTUAL"
	 * </pre>
	 * 
	 * @param opcodes
	 *            Array of opcode names to search for.
	 * @return Opcode pattern parameter.
	 */
	public static Parameter opcodes(String... opcodes) {
		return opcodes(Arrays.asList(opcodes));
	}

	/**
	 * Search for an array of opcodes.
	 * 
	 * For instance: <i>(Hello world opcodes)</i>
	 * 
	 * <pre>
	 * Arrays.asList("GETSTATIC", "LDC", "INVOKEVIRTUAL")
	 * </pre>
	 * 
	 * @param opcodes
	 *            List of opcode names to search for.
	 * @return Opcode pattern parameter.
	 */
	public static Parameter opcodes(List<String> opcodes) {
		return new Parameter(SearchType.OPCODE_PATTERN, opcodes);
	}

	/**
	 * Search for a declaration of a class by the given <i>(partial)</i>
	 * name.
	 * 
	 * @param owner
	 *            <i>(Partial)</i> Class name.
	 * @return Declared class.
	 */
	public static Parameter declaration(String owner) {
		return new Parameter(SearchType.DECLARATION, owner);
	}

	/**
	 * Search for a declaration by the given owner, name, and descriptor.
	 * All fields are optional. To disregard an item, pass {@code null}.
	 * 
	 * @param owner
	 *            Class that owns the member.
	 * @param name
	 *            Member name.
	 * @param desc
	 *            Member type descriptor.
	 * @return Declared member parameter.
	 */
	public static Parameter declaration(String owner, String name, String desc) {
		return new Parameter(SearchType.DECLARATION, owner, name, desc);
	}

	/**
	 * Search for references to member(s) defined by the given owner, name,
	 * and descriptor. All fields are optional. To disregard an item, pass
	 * {@code null}. <br>
	 * Searching only for the owner for example will show all references to
	 * that class. Searching with only the descriptor will show all
	 * references to members of that type.
	 * 
	 * @param owner
	 *            Class that owns the member.
	 * @param name
	 *            Member name.
	 * @param desc
	 *            Member type descriptor.
	 * @return Referred member parameter.
	 */
	public static Parameter references(String owner, String name, String desc) {
		return new Parameter(SearchType.REFERENCE, owner, name, desc);
	}
}