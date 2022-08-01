package me.coley.recaf.assemble.suggestions;

import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.ast.arch.MethodParameter;
import me.coley.recaf.assemble.util.ProgramClass;
import me.coley.recaf.assemble.util.ProgramField;
import me.coley.recaf.assemble.util.ProgramMethod;
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


public class Suggestions {

	private final Function<String, ProgramClass> mapper;
	private final Tree systemClasses = ClasspathUtil.getSystemClasses();
	private final Tree classes;
	private MethodDefinition method;
	public final static TreeSet<String> instructions = new TreeSet<>(ParseInfo.actions.keySet());
	private final static SuggestionResult EMPTY = new SuggestionResult("", Stream.empty());

	/**
	 * @param classes
	 * 		a tree of current user classes
	 * @param mapper
	 * 		a function that maps a class name to a {@link ProgramClass} instance
	 */
	public Suggestions(Tree classes, Function<String, ProgramClass> mapper, MethodDefinition method) {
		this.classes = classes;
		this.mapper = mapper;
		this.method = method;
	}

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
	public SuggestionResult getSuggestion(Group group) {
		if (group == null) {
			return new SuggestionResult("", instructions.stream());
		}
		switch (group.type) {
			case INSTRUCTION:
				return getInstructionSuggestion(group);
			case IDENTIFIER: {
				String content = group.content();
				return new SuggestionResult(
						group.content(),
						instructions.stream().filter(s -> !s.equals(content) && s.startsWith(content))
				);
			}

		}
		// return empty suggestion result
		return new SuggestionResult("", Stream.empty());
	}

	private SuggestionResult getInstructionSuggestion(Group group) {
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
			case "invokevirtual": {
				// first child is class.name so we have to suggest class name until the first .
				String className = children[0] == null ? "" : children[0].content();
				if (!className.contains(".")) return startsWith(className);
				String[] parts = className.split("\\.");
				ProgramClass clazz = mapper.apply(parts[0]);
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
				return new SuggestionResult(children[0].content(),
						locals.stream().filter(s -> s.startsWith(localName)));
			}
			case "getstatic":
			case "putstatic":
			case "getfield":
			case "putfield": {
				String className = children[0] == null ? "" : children[0].content();
				if (!className.contains(".")) return startsWith(className);
				String[] parts = className.split("\\.");
				ProgramClass clazz = mapper.apply(parts[0]);
				if (clazz == null) return EMPTY;
				String fieldName = parts.length == 1 ? "" : parts[1];
				return getFieldSuggestion(clazz, inst.contains("static"), fieldName);
			}
		}
		return EMPTY;
	}

	private SuggestionResult getFieldSuggestion(ProgramClass clazz, boolean isStatic, String fieldName) {
		Stream<ProgramField> fields = isStatic ?
				clazz.getFields().stream().filter(m -> (m.getAccess() & Opcodes.ACC_STATIC) != 0)
				: clazz.getFields().stream().filter(m -> (m.getAccess() & Opcodes.ACC_STATIC) == 0);
		return new SuggestionResult(
				fieldName,
				fields.map(m -> m.getName() + " " + m.getDescriptor())
						.filter(name -> !name.equals(fieldName) && name.startsWith(fieldName))
		);
	}

	private SuggestionResult getMethodSuggestion(ProgramClass clazz, boolean isStatic, String methodName) {
		Stream<ProgramMethod> methods = isStatic ?
				clazz.getMethods().stream().filter(m -> (m.getAccess() & Opcodes.ACC_STATIC) != 0)
				: clazz.getMethods().stream().filter(m -> (m.getAccess() & Opcodes.ACC_STATIC) == 0);
		return new SuggestionResult(
				methodName,
				methods.map(m -> m.getName() + " " + m.getDescriptor())
						.filter(name -> !name.equals(methodName) && name.startsWith(methodName))
		);
	}

	private SuggestionResult startsWith(String partial) {
		return new SuggestionResult(
				partial,
				Stream.concat(
						searchIn(classes, partial),
						searchIn(systemClasses, partial)
				)
		);
	}

	private Stream<String> searchIn(Tree root, String partial) {
		return root.getAllLeaves()
				.map(Tree::getFullValue)
				.filter(x -> !x.equals(partial) && x.startsWith(partial));
	}
}
