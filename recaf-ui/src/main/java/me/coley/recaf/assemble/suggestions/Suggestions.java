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

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
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
	private final static SuggestionsResults EMPTY = new SuggestionsResults("", Stream.empty());
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
				return startsWith(children[0] == null ? "" : children[0].content());
			case "invokestatic":
			case "invokeinterface":
			case "invokespecial":
			case "invokevirtual":
			case "invokevirtualinterface":
			case "invokedynamic": {
				// first child is class.name, so we have to suggest class name until the first .
				String className = children[0] == null ? "" : children[0].content();
				if (!className.contains(".")) return startsWith(className);
				String[] parts = className.split("\\.");
				CommonClassInfo clazz = mapper.apply(parts[0]);
				if (clazz == null) return EMPTY;
				String methodName = parts.length == 1 ? "" : parts[1];
				return getMethodSuggestion(clazz, instruction.content().equals("invokestatic"), methodName);
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
				String localName = children[0] == null ? "" : children[0].content();

				Set<String> locals = new HashSet<>();
				if (!AccessFlag.isStatic(method.getModifiers().value()))
					locals.add("this");
				for (MethodParameter parameter : method.getParams().getParameters()) {
					locals.add(parameter.getName());
				}
				return new SuggestionsResults(children[0].content(),
						locals.stream()
								.filter(s -> s.startsWith(localName))
								.map(i -> new Suggestion(null, i)));
			}
			case "getstatic":
			case "putstatic":
			case "getfield":
			case "putfield": {
				String className = children[0] == null ? "" : children[0].content();
				if (!className.contains(".")) return startsWith(className);
				String[] parts = className.split("\\.");
				CommonClassInfo clazz = mapper.apply(parts[0]);
				if (clazz == null) return EMPTY;
				String fieldName = parts.length == 1 ? "" : parts[1];
				return getFieldSuggestion(clazz, inst.contains("static"), fieldName);
			}
		}
		return EMPTY;
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
		return new SuggestionsResults(
				partial,
				Stream.concat(
						searchIn(classes, partial),
						searchIn(systemClasses, partial)
				).map(c -> new Suggestion(mapper.apply(c), c))
		);
	}

	private Stream<String> searchIn(Tree root, String partial) {
		return root.getAllLeaves()
				.map(Tree::getFullValue)
				.filter(x -> !x.equals(partial) && x.startsWith(partial));
	}
}
