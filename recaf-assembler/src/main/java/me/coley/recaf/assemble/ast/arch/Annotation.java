package me.coley.recaf.assemble.ast.arch;

import me.coley.recaf.assemble.ast.*;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Details an annotation. This does not cover <i>type-annotations</i> which have more details.
 *
 * @author Matt Coley
 */
public class Annotation extends BaseElement implements CodeEntry {
	private final String type;
	private final Map<String, AnnoArg> args;
	private final boolean isVisible;

	/**
	 * @param isVisible
	 *        {@code true} if the annotation is intended to be source-level visible.
	 * @param type
	 * 		Annotation class type.
	 * @param args
	 * 		Map of arg names to values.
	 */
	public Annotation(boolean isVisible, String type, Map<String, AnnoArg> args) {
		this.isVisible = isVisible;
		this.type = type;
		this.args = args;
	}

	/**
	 * @return {@code true} if the annotation is intended to be source-level visible.
	 */
	public boolean isVisible() {
		return isVisible;
	}

	/**
	 * @return Annotation class type.
	 */
	public String getType() {
		return type;
	}

	/**
	 * @return Map of arg names to values.
	 */
	public Map<String, AnnoArg> getArgs() {
		return args;
	}

	@Override
	public void insertInto(Code code) {
		code.add(this);
	}

	@Override
	public String print(PrintContext context) {
		String op = isVisible ? "annotation" : "invisible-annotation";
		if(args.isEmpty()) return context.fmtKeyword(op) + " " + type + " " + context.fmtKeyword("end\n");
		String argsString = args.entrySet().stream()
				.map(e -> e.getKey() + " " + e.getValue().print(context))
				.collect(Collectors.joining("\n"));
		return String.format("%s %s\n %s\n%s\n ", context.fmtKeyword(op),
				type, argsString, context.fmtKeyword("end"));
	}

	/**
	 * Arg representing enums.
	 */
	public static class AnnoEnum extends AnnoArg {
		private final String type;
		private final String name;

		/**
		 * @param type
		 * 		Enum type name.
		 * @param name
		 * 		Enum value name.
		 */
		public AnnoEnum(String type, String name) {
			super(ArgType.ANNO_ENUM, type + " " + name);
			this.type = type;
			this.name = name;
		}

		/**
		 * @return Enum type name.
		 */
		public String getEnumType() {
			return type;
		}

		/**
		 * @return Enum value name.
		 */
		public String getEnumName() {
			return name;
		}
	}

	/**
	 * Wrapper for annotation args.
	 */
	public static class AnnoArg extends BaseArg {
		/**
		 * @param type
		 * 		Type of value.
		 * @param value
		 * 		Value instance.
		 */
		public AnnoArg(ArgType type, Object value) {
			super(type, value);
		}
	}
}
