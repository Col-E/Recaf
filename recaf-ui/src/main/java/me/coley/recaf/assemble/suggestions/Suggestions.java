package me.coley.recaf.assemble.suggestions;

import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.ast.arch.MethodParameter;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.ClasspathUtil;
import me.darknet.assembler.instructions.ParseInfo;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.groups.InstructionGroup;
import org.objectweb.asm.Opcodes;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static me.coley.recaf.util.ClasspathUtil.Tree;

/**
 * Suggestion text provider.
 *
 * @author Nowilltolife
 * @author xDark
 */
public class Suggestions {
	private final static TreeSet<String> INSTRUCTIONS = new TreeSet<>(ParseInfo.actions.keySet());
	private final Function<String, CommonClassInfo> mapper;
	private final Tree systemClasses = ClasspathUtil.getSystemClasses();
	private final Tree classes;
	private MethodDefinition method;

	/**
	 * @param classes
	 * 		Tree of current user classes.
	 * @param mapper
	 * 		Function that maps a class name to a {@link CommonClassInfo} instance.
	 */
	public Suggestions(Tree classes, Function<String, CommonClassInfo> mapper, MethodDefinition method) {
		this.classes = classes;
		this.mapper = mapper;
		this.method = method;
	}

	/**
	 * @param method
	 * 		Method context.
	 */
	public void setMethod(MethodDefinition method) {
		this.method = method;
	}

	/**
	 * Supply suggestions for a given group and partial parameters.
	 *
	 * @param group
	 * 		Group to suggest for.
	 *
	 * @return Suggestions for the given group.
	 */
	public SuggestionsResults getSuggestion(Group group) {
		if (group == null) {
			return new SuggestionsResults("", INSTRUCTIONS.stream()
					.map(i -> new Suggestion(null, i)));
		}
		switch (group.type) {
			case INSTRUCTION:
				return getInstructionSuggestion(group);
			case IDENTIFIER: {
				String content = group.content();
				return new SuggestionsResults(
						group.content(),
						INSTRUCTIONS.stream()
								.filter(s -> !s.equals(content) && s.startsWith(content))
								.map(i -> new Suggestion(null, i))
				);
			}

		}
		// return empty suggestion result
		return new SuggestionsResults("", Stream.empty());
	}

	private SuggestionsResults getInstructionSuggestion(Group group) {
		InstructionGroup instruction = (InstructionGroup) group;
		Group[] children = instruction.children;
		String inst = instruction.content();
		switch (instruction.content()) {
			case "new":
			case "anewarray":
			case "checkcast":
			case "instanceof":
				return advancedSearch(children[0] == null ? "" : children[0].content());
			case "invokestatic":
			case "invokeinterface":
			case "invokespecial":
			case "invokevirtual":
			case "invokevirtualinterface":
			case "invokedynamic": {
				// first child is class.name, so we have to suggest class name until the first .
				return getInstructionSuggestionsResults(children, inst,
						(clazz, methodName) -> getMethodSuggestion(clazz, instruction.content().equals("invokestatic"), methodName));
			}
			case "iload":
			case "lload":
			case "fload":
			case "dload":
			case "aload":
			case "istore":
			case "lstore":
			case "fstore":
			case "dstore":
			case "astore": {
				String search = children[0] == null ? "" : children[0].content();
				Set<String> locals = new HashSet<>();
				if (!AccessFlag.isStatic(method.getModifiers().value()))
					locals.add("this");
				for (MethodParameter parameter : method.getParams().getParameters()) {
					locals.add(parameter.getName());
				}
				return new SuggestionsResults(children[0].content(),
						search.isEmpty() ?
								locals.stream().map(ln -> new Suggestion(null, ln)) :
								locals.stream().map(localName -> {
											BitSet bs = matches(search, localName);
											if (bs == null) return null;
											return new Suggestion(null, localName, bs);
										})
										.filter(Objects::nonNull));
			}
			case "getstatic":
			case "putstatic":
			case "getfield":
			case "putfield": {
				return getInstructionSuggestionsResults(children, inst,
						(clazz, fieldName) -> getFieldSuggestion(clazz, inst.contains("static"), fieldName));
			}
		}
		return new SuggestionsResults(inst, Stream.empty());
	}

	private SuggestionsResults advancedSearch(String search) {
		if (search.isBlank())
			return new SuggestionsResults(search, getAllClasses().map(c -> new Suggestion(mapper.apply(c), c)));
		return new SuggestionsResults(search, getAllClasses().map(className -> {
			if (className.equals(search)) return null;
			final BitSet bitSet = matches(className, search);
			if (bitSet == null) return null;
			return new Suggestion(mapper.apply(className), className, bitSet);
		}).filter(Objects::nonNull));
	}

	private SuggestionsResults getInstructionSuggestionsResults(Group[] children, String inst, BiFunction<CommonClassInfo, String, SuggestionsResults> suggestionMaker) {
		String className = children[0] == null ? "" : children[0].content();
		if (!className.contains(".")) return advancedSearch(className);
		String[] parts = className.split("\\.");
		CommonClassInfo clazz = mapper.apply(parts[0]);
		if (clazz == null) return new SuggestionsResults(inst, Stream.empty());
		return suggestionMaker.apply(clazz, parts.length == 1 ? "" : parts[1]);
	}

	private SuggestionsResults getFieldSuggestion(CommonClassInfo clazz, boolean isStatic, String fieldName) {
		Stream<FieldInfo> fields = isStatic ?
				clazz.getFields().stream().filter(m -> (m.getAccess() & Opcodes.ACC_STATIC) != 0)
				: clazz.getFields().stream().filter(m -> (m.getAccess() & Opcodes.ACC_STATIC) == 0);
		return new SuggestionsResults(
				fieldName,
				fields.filter(f -> !f.getName().equals(fieldName) && format(f).startsWith(fieldName))
						.map(f -> new Suggestion(f, format(f)))
		);
	}


	private SuggestionsResults getMethodSuggestion(CommonClassInfo clazz, boolean isStatic, String methodName) {
		Stream<MethodInfo> methods = isStatic ?
				clazz.getMethods().stream().filter(m -> (m.getAccess() & Opcodes.ACC_STATIC) != 0)
				: clazz.getMethods().stream().filter(m -> (m.getAccess() & Opcodes.ACC_STATIC) == 0);
		return new SuggestionsResults(
				methodName,
				methods.filter(m -> !m.getName().equals(methodName) && format(m).startsWith(methodName))
						.map(m -> new Suggestion(m, format(m)))
		);
	}

	private static String format(FieldInfo info) {
		return info.getName() + " " + info.getDescriptor();
	}

	private static String format(MethodInfo info) {
		return info.getName() + " " + info.getDescriptor();
	}

	private SuggestionsResults startsWith(String partial) {
		return new SuggestionsResults(partial,
				(
						"".equals(partial) ?
								getAllClasses() :
								getAllClasses().filter(x -> !x.equals(partial) && x.startsWith(partial))
				).map(c -> new Suggestion(mapper.apply(c), c))
		);
	}

	private Stream<String> getAllClasses() {
		return Stream.concat(classes.getAllLeaves()
				.map(Tree::getFullValue), systemClasses.getAllLeaves()
				.map(Tree::getFullValue));
	}

	/**
	 * @param text
	 * 		Text to match over.
	 * @param search
	 * 		Search text.
	 *
	 * @return {@code null} when no match, otherwise the chars matched
	 */
	@Nullable
	private static BitSet matches(String text, String search) {
		BitSet matches = new BitSet();
		int idx = -1;
		for (int i = 0, j = search.length(); i < j; i++) {
			char ch = search.charAt(i);
			if ((idx = text.indexOf(ch, idx + 1)) == -1) {
				return null;
			}
			matches.set(idx);
		}
		return matches;
	}
}
