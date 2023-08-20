package me.coley.recaf.assemble.suggestions;

import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.ast.arch.MethodParameter;
import me.coley.recaf.assemble.suggestions.type.*;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.MemberInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.ClasspathUtil;
import me.coley.recaf.util.EscapeUtil;
import me.darknet.assembler.instructions.ParseInfo;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.groups.instructions.InstructionGroup;
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
 * @author Justus Garbe
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
			return new SuggestionsResults("", INSTRUCTIONS.stream().map(StringSuggestion::new));
		}
		switch (group.getType()) {
			case INSTRUCTION:
				return getInstructionSuggestion(group);
			case IDENTIFIER: {
				String content = group.content();
				return new SuggestionsResults(
						group.content(),
						INSTRUCTIONS.stream()
								.filter(s -> !s.equals(content) && s.startsWith(content))
								.map(insn -> new StringMatchSuggestion(content, insn))
				);
			}

		}
		// return empty suggestion result
		return new SuggestionsResults("", Stream.empty());
	}

	private SuggestionsResults getInstructionSuggestion(Group group) {
		InstructionGroup instruction = (InstructionGroup) group;
		List<Group> children = instruction.getChildren();
		String inst = instruction.content();
		String first = children.isEmpty() ? "" : children.get(0) == null ? "" : children.get(0).content();
		switch (instruction.content()) {
			case "new":
			case "anewarray":
			case "checkcast":
			case "instanceof":
				return fuzzySearchClasses(first);
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
				Set<String> locals = new HashSet<>();
				if (!AccessFlag.isStatic(method.getModifiers().value()))
					locals.add("this");
				for (MethodParameter parameter : method.getParams().getParameters()) {
					locals.add(parameter.getName());
				}
				return new SuggestionsResults(first, fuzzySearchStrings(first, locals));
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

	private SuggestionsResults getInstructionSuggestionsResults(List<Group> children, String inst, BiFunction<CommonClassInfo, String, SuggestionsResults> suggestionMaker) {
		String className = children.isEmpty() ? "" : children.get(0) == null ? "" : children.get(0).content();
		if (!className.contains(".")) return fuzzySearchClasses(className);
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
				startsWithSearchFields(fieldName, fields)
		);
	}

	private SuggestionsResults getMethodSuggestion(CommonClassInfo clazz, boolean isStatic, String methodName) {
		Stream<MethodInfo> methods = isStatic ?
				clazz.getMethods().stream().filter(m -> (m.getAccess() & Opcodes.ACC_STATIC) != 0)
				: clazz.getMethods().stream().filter(m -> (m.getAccess() & Opcodes.ACC_STATIC) == 0);
		return new SuggestionsResults(
				methodName,
				startsWithSearchMethods(methodName, methods)
		);
	}

	private static Stream<MemberInfoSuggestion> startsWithSearchMethods(String methodName, Stream<MethodInfo> methods) {
		return startsWithSearchInfo(methodName, methods, Suggestions::format);
	}

	private static Stream<MemberInfoSuggestion> startsWithSearchFields(String fieldName, Stream<FieldInfo> fields) {
		return startsWithSearchInfo(fieldName, fields, Suggestions::format);
	}

	private static <I extends MemberInfo> Stream<MemberInfoSuggestion> startsWithSearchInfo(
			String search, Stream<I> infoStream,
			Function<I, String> formatInfo) {
		return infoStream.filter(f -> !f.getName().equals(search))
				.map(f -> {
					if (search.isBlank()) {
						return new MemberInfoSuggestion(search, f, null);
					} else {
						String fmt = formatInfo.apply(f);
						BitSet matchingChars = matches(fmt, search);
						if (matchingChars == null)
							return null;
						return new MemberInfoSuggestion(search, f, matchingChars);
					}
				}).filter(Objects::nonNull);
	}

	private static Stream<? extends Suggestion> fuzzySearchStrings(String search, Set<String> locals) {
		return search.isEmpty() ?
				locals.stream().map(ln -> new StringMatchSuggestion(search, ln)) :
				locals.stream().map(localName -> {
							BitSet bs = matches(search, localName);
							if (bs == null) return null;
							return new StringMatchSuggestion(search, localName, bs);
						})
						.filter(Objects::nonNull);
	}

	private static String format(FieldInfo info) {
		return info.getName() + " " + info.getDescriptor();
	}

	private static String format(MethodInfo info) {
		return EscapeUtil.formatIdentifier(info.getName()) + " " + EscapeUtil.formatIdentifier(info.getDescriptor());
	}

	private Suggestion createClassSuggestion(String partial, String c, @Nullable BitSet bitSet) {
		final CommonClassInfo info = mapper.apply(c);
		return info == null ? new StringMatchSuggestion(partial, c, bitSet) : new InfoSuggestion(partial, info, bitSet);
	}

	private SuggestionsResults fuzzySearchClasses(String search) {
		if (search.isBlank())
			return new SuggestionsResults(search, getAllClasses().map(c -> createClassSuggestion(search, c, null)));
		return new SuggestionsResults(search, getAllClasses().map(className -> {
			if (className.equals(search)) return null;
			final BitSet bitSet = matches(className, search);
			if (bitSet == null) return null;
			return createClassSuggestion(search, className, bitSet);
		}).filter(Objects::nonNull));
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
