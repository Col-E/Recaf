package software.coley.recaf.services.mapping.format;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import software.coley.collections.tuple.Pair;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.Mappings;
import software.coley.recaf.services.mapping.data.ClassMapping;
import software.coley.recaf.services.mapping.data.FieldMapping;
import software.coley.recaf.services.mapping.data.MethodMapping;
import software.coley.recaf.services.mapping.data.VariableMapping;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.util.Types;
import software.coley.recaf.workspace.model.Workspace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Enigma mappings file implementation.
 * <p>
 * Specification: <a href="https://wiki.fabricmc.net/documentation:enigma_mappings">enigma_mappings</a>
 *
 * @author Janmm14
 * @author Matt Coley
 */
@Dependent
public class EnigmaMappings extends AbstractMappingFileFormat {
	public static final String NAME = "Enigma";
	private static final String FAIL = "Invalid Enigma mappings, ";
	private static final Logger LOGGER = Logging.get(EnigmaMappings.class);
	// Parser phase constants
	private static final int PHASE_IGNORE_LINE = 0;
	private static final int PHASE_FIND_TYPE = 1;
	private static final int PHASE_TYPE_CLASS = 2;
	private static final int PHASE_TYPE_FIELD = 3;
	private static final int PHASE_TYPE_METHOD = 4;
	// The finishing flag needs to be higher than the highest phase, as it is an additive flag
	private static final int PHASE_TYPE_FLAG_FINISH = 8;

	/**
	 * New enigma instance.
	 */
	public EnigmaMappings() {
		super(NAME, true, true);
	}

	/**
	 * New enigma instance with workspace context.
	 *
	 * @param workspaceManager
	 * 		Workspace manager to pull the current workspace from.
	 */
	@Inject
	public EnigmaMappings(@Nonnull WorkspaceManager workspaceManager) {
		this();
		if (workspaceManager.hasCurrentWorkspace())
			setWorkspace(workspaceManager.getCurrent());
	}

	/**
	 * Parses an Enigma file, or Enigma directory containing multiple enigma mapping files, suffixed with {@code .mapping}.
	 * <br>
	 * See for instance: <a href="https://github.com/FabricMC/yarn/mappings">FabricMC/yarn</a>
	 *
	 * @param path
	 * 		Root file/directory of enigma mappings.
	 *
	 * @return Intermediate mappings from parsed enigma file/directory.
	 *
	 * @throws InvalidMappingException
	 * 		When reading the mappings encounters any failure.
	 */
	@Nonnull
	public IntermediateMappings parse(@Nonnull Path path) throws InvalidMappingException {
		if (Files.isRegularFile(path)) {
			try {
				return parse(Files.readString(path));
			} catch (IOException ex) {
				throw new InvalidMappingException(ex);
			}
		}

		IntermediateMappings sum = new IntermediateMappings();
		try (Stream<Path> files = Files.walk(path).filter(p -> p.getFileName().toString().endsWith(".mapping"))) {
			files.forEach(p -> {
				try {
					String fileContents = Files.readString(p);
					IntermediateMappings mappings = parse(fileContents);
					sum.putAll(mappings);
				} catch (Exception ex) {
					// Rethrow so outer catch will handle
					throw new IllegalStateException(ex);
				}
			});
		} catch (Throwable ex) {
			throw new InvalidMappingException("Failed to walk enigma directory: " + path, ex);
		}
		return sum;
	}

	@Nonnull
	@Override
	public IntermediateMappings parse(@Nonnull String mappingsText) throws InvalidMappingException {
		return parseEnigma(mappingsText);
	}

	@Override
	public String exportText(@Nonnull Mappings mappings) throws InvalidMappingException {
		return exportHierarchy(mappings.exportIntermediate(), getWorkspace());
	}

	/**
	 * @param mappingsText
	 * 		Text of the mappings to parse.
	 *
	 * @return Intermediate mappings from parsed text.
	 *
	 * @throws InvalidMappingException
	 * 		When reading the mappings encounters any failure.
	 */
	@Nonnull
	public static IntermediateMappings parseEnigma(@Nonnull String mappingsText) throws InvalidMappingException {
		// COMMENT comment #ignored
		// CLASS BaseClass TargetClass
		//     FIELD baseField targetField baseDesc
		//     METHOD baseMethod targetMethod baseMethodDesc
		//         ARG index targetArg
		//         VAR desc baseVar targetVar
		//     CLASS 1 InnerClass
		//         FIELD innerField targetField innerDesc
		IntermediateMappings mappings = new IntermediateMappings();
		ParseContext context = new ParseContext();

		int line = 1;
		for (int i = 0, len = mappingsText.length(); i < len; ) { // i incremented inside the loop
			// count \t
			int indent = 0;
			for (; i < len; i++) {
				char c = mappingsText.charAt(i);
				if (c == '\t') {
					indent++;
					continue;
				}
				break;
			}

			// parse line
			i = handleLine(line, indent, i, mappingsText, context, mappings);

			// go to next line
			if (i < len) {
				char c = mappingsText.charAt(i);
				assert c == '\n' || c == '\r' : "Expected newline, got <" + c + "> (" + ((int) c) + ") @line " + line + " @char " + i;
				line++;
				if (c == '\r') {
					int ip1 = i + 1;
					if (ip1 < len && mappingsText.charAt(ip1) == '\n') {
						i++;
					}
				}
				i++;
			}
		}
		return mappings;
	}

	/**
	 * @param line
	 * 		Current line number in the mappings file <i>(1 based)</i>
	 * @param indent
	 * 		Current level of indentation. Should be equal to or less than the size of the {@code currentClass} {@link Deque}.
	 * @param i
	 * 		Current offset into the mappings file.
	 * @param mappingsText
	 * 		Mappings file contents.
	 * @param context
	 * 		Current class and method parser context.
	 * @param mappings
	 * 		Output mappings.
	 *
	 * @return Updated offset into the mappings file.
	 *
	 * @throws InvalidMappingException
	 * 		When reading the mappings encounters any failure.
	 */
	private static int handleLine(int line, int indent, int i, @Nonnull String mappingsText, @Nonnull ParseContext context,
	                              @Nonnull IntermediateMappings mappings) throws InvalidMappingException {
		// read next token
		String lineType = mappingsText.substring(i = skipSpace(i, mappingsText), i = readToken(i, mappingsText));
		MethodContext currentMethod = context.currentMethod;
		if (currentMethod != null && indent <= currentMethod.indent)
			context.currentMethod = null;
		switch (lineType) {
			case "CLASS" -> {
				Deque<Pair<String, String>> currentClass = context.currentClass;
				updateIndent(currentClass, indent, () -> ("Invalid Enigma mappings, CLASS indent level " + indent + " too deep (expected max. "
						+ currentClass.size() + ", " + currentClass + ") @line " + line + " @char "), i);

				String classNameA = mappingsText.substring(i = skipSpace(i, mappingsText), i = readToken(i, mappingsText));
				classNameA = removeNonePackage(classNameA);
				classNameA = qualifyWithOuterClassesA(currentClass, classNameA);

				String classNameB = mappingsText.substring(i = skipSpace(i, mappingsText), i = readToken(i, mappingsText));
				if (classNameB.isEmpty() || "-".equals(classNameB) || classNameB.startsWith("ACC:")) {
					// no mapping for class, but need to include for context for following members
					classNameB = classNameA;
				} else {
					classNameB = removeNonePackage(classNameB);
					classNameB = qualifyWithOuterClassesB(currentClass, classNameB);
					mappings.addClass(classNameA, classNameB);
				}
				currentClass.push(new Pair<>(classNameA, classNameB));
			}
			case "FIELD" -> {
				MemberMappingInfo info = handleClassMemberMapping(line, indent, i, mappingsText, context.currentClass, "FIELD", mappings::addField);
				i = info.offset;
			}
			case "METHOD" -> {
				MemberMappingInfo info = handleClassMemberMapping(line, indent, i, mappingsText, context.currentClass, "METHOD", mappings::addMethod);
				i = info.offset;
				if (info.isValid())
					context.currentMethod = new MethodContext(indent, info.ownerName, info.oldName, info.desc);
			}
			case "ARG" -> i = handleArgumentMapping(i, mappingsText, context.currentMethod, mappings);
			case "VAR" -> i = handleVariableMapping(i, mappingsText, context.currentMethod, mappings);
		}
		i = skipLineRest(i, mappingsText);
		return i;
	}

	/**
	 * @param line
	 * 		Current line number in the mappings file <i>(1 based)</i>
	 * @param indent
	 * 		Current level of indentation. Should be equal to or less than the size of the {@code currentClass} {@link Deque}.
	 * @param i
	 * 		Current offset into the mappings file.
	 * @param mappingsText
	 * 		Mappings file contents.
	 * @param currentClass
	 * 		Deque of the current 'context' <i>(what class are we building mappings for)</i>.
	 * @param type
	 * 		The expected type of content we're handling. IE, {@code CLASS}, {@code FIELD}, or {@code METHOD}.
	 * @param consumer
	 * 		Consumer to record parsed mappings into.
	 *
	 * @return Member mapping line information.
	 *
	 * @throws InvalidMappingException
	 * 		When reading the mappings encounters any failure.
	 */
	@Nonnull
	private static MemberMappingInfo handleClassMemberMapping(int line, int indent, int i, @Nonnull String mappingsText, @Nonnull Deque<Pair<String, String>> currentClass,
	                                                          @Nonnull String type, @Nonnull MemberMappingsConsumer consumer) throws InvalidMappingException {
		// <name-a> <name-b> <formatted-access-modifier> <desc> <eol>
		// <name-b> = '' | '-' | <space> <name>
		updateIndent(currentClass, indent, () -> FAIL + type + " indent level " + indent + " too deep (expected max. "
				+ currentClass.size() + ", " + currentClass + ") @line " + line + " @char ", i);
		if (currentClass.isEmpty()) {
			throw new InvalidMappingException(FAIL + type + " without class context @line " + line + " @char " + i);
		}

		String nameA = mappingsText.substring(i = skipSpace(i, mappingsText), i = readToken(i, mappingsText));
		String next = mappingsText.substring(i = skipSpace(i, mappingsText), i = readToken(i, mappingsText));
		if (nameA.isEmpty() || next.isEmpty())
			return new MemberMappingInfo(i);

		// Handle the case where the next token is a descriptor, which means the nameB is omitted.
		// - Also skip any access modifiers, as they are not relevant to the mapping.
		String nameB = null;
		String desc;
		if ("-".equals(next)) {
			desc = mappingsText.substring(i = skipSpace(i, mappingsText), i = readToken(i, mappingsText));
		} else if (next.startsWith("ACC:")) {
			desc = next;
		} else if (Types.isValidDesc(next)) {
			desc = next;
		} else {
			nameB = next;
			desc = mappingsText.substring(i = skipSpace(i, mappingsText), i = readToken(i, mappingsText));
		}
		while (desc.startsWith("ACC:"))
			desc = mappingsText.substring(i = skipSpace(i, mappingsText), i = readToken(i, mappingsText));
		if (desc.isEmpty())
			return new MemberMappingInfo(i);

		// Record the mapping if we have a valid nameB.
		String ownerName = currentClass.peek().getLeft();
		if (nameB != null)
			consumer.accept(ownerName, desc, nameA, nameB);
		return new MemberMappingInfo(i, ownerName, nameA, desc);
	}

	/**
	 * @param i
	 * 		Current offset into the mappings file.
	 * @param mappingsText
	 * 		Mappings file contents.
	 * @param currentMethod
	 * 		Current method context to attach the argument mapping to.
	 * @param mappings
	 * 		Output mappings.
	 *
	 * @return Updated offset into the mappings file.
	 *
	 * @throws InvalidMappingException
	 * 		When reading the mappings encounters any failure.
	 */
	private static int handleArgumentMapping(int i, @Nonnull String mappingsText, @Nullable MethodContext currentMethod,
	                                         @Nonnull IntermediateMappings mappings) throws InvalidMappingException {
		String indexText = mappingsText.substring(i = skipSpace(i, mappingsText), i = readToken(i, mappingsText));
		String newName = mappingsText.substring(i = skipSpace(i, mappingsText), i = readToken(i, mappingsText));
		if (currentMethod != null && !indexText.isEmpty() && !newName.isEmpty()) {
			try {
				int index = Integer.parseInt(indexText);
				mappings.addVariable(currentMethod.ownerName, currentMethod.methodName, currentMethod.methodDesc,
						null, null, index, newName);
			} catch (NumberFormatException ignored) {
				// Skip invalid index, as it is not a valid mapping.
			}
		}
		return i;
	}

	/**
	 * @param i
	 * 		Current offset into the mappings file.
	 * @param mappingsText
	 * 		Mappings file contents.
	 * @param currentMethod
	 * 		Current method context to attach the variable mapping to.
	 * @param mappings
	 * 		Output mappings.
	 *
	 * @return Updated offset into the mappings file.
	 *
	 * @throws InvalidMappingException
	 * 		When reading the mappings encounters any failure.
	 */
	private static int handleVariableMapping(int i, @Nonnull String mappingsText, @Nullable MethodContext currentMethod,
	                                         @Nonnull IntermediateMappings mappings) throws InvalidMappingException {
		String desc = mappingsText.substring(i = skipSpace(i, mappingsText), i = readToken(i, mappingsText));
		String oldName = mappingsText.substring(i = skipSpace(i, mappingsText), i = readToken(i, mappingsText));
		String newName = mappingsText.substring(i = skipSpace(i, mappingsText), i = readToken(i, mappingsText));
		if (currentMethod != null && !desc.isEmpty() && !oldName.isEmpty() && !newName.isEmpty()) {
			mappings.addVariable(currentMethod.ownerName, currentMethod.methodName, currentMethod.methodDesc,
					desc, oldName, -1, newName);
		}
		return i;
	}

	/**
	 * @param currentClass
	 * 		Deque of the current 'context' <i>(what class are we building mappings for)</i>.
	 * @param classNameA
	 * 		Initial class name.
	 *
	 * @return Fully qualified class name based on the current context in the deque.
	 */
	@Nonnull
	private static String qualifyWithOuterClassesA(@Nonnull Deque<Pair<String, String>> currentClass, @Nonnull String classNameA) {
		if (currentClass.isEmpty()) {
			return classNameA;
		}
		StringBuilder sb = new StringBuilder();
		for (Pair<String, String> pair : currentClass) {
			sb.append(pair.getLeft()).append('$');
		}
		classNameA = sb.append(classNameA).toString();
		return classNameA;
	}

	/**
	 * @param currentClass
	 * 		Deque of the current 'context' <i>(what class are we building mappings for)</i>.
	 * @param classNameB
	 * 		Destination class name.
	 *
	 * @return Fully qualified class name based on the current context in the deque.
	 */
	@Nonnull
	private static String qualifyWithOuterClassesB(@Nonnull Deque<Pair<String, String>> currentClass, @Nonnull String classNameB) {
		if (currentClass.isEmpty()) {
			return classNameB;
		}
		StringBuilder sb = new StringBuilder();
		for (Pair<String, String> pair : currentClass) {
			sb.append(pair.getRight()).append('$');
		}
		classNameB = sb.append(classNameB).toString();
		return classNameB;
	}

	/**
	 * @param currentClass
	 * 		Deque of the current 'context' <i>(what class are we building mappings for)</i>.
	 * @param indent
	 * 		Current level of indentation. Should be equal to or less than the size of the {@code currentClass} {@link Deque}.
	 * @param failStr
	 * 		Message to include in the thrown invalid mapping exception if the indentation state is invalid.
	 * @param i
	 * 		Current offset into the mappings file.
	 *
	 * @throws InvalidMappingException
	 * 		Thrown when the indentation state does not match the current class context.
	 */
	private static void updateIndent(@Nonnull Deque<Pair<String, String>> currentClass, int indent, @Nonnull Supplier<String> failStr, int i) throws InvalidMappingException {
		if (indent > currentClass.size()) {
			throw new InvalidMappingException(failStr.get() + i);
		}
		while (currentClass.size() > indent) {
			currentClass.pop();
		}
	}

	/**
	 * @param i
	 * 		Current offset into the mappings file.
	 * @param mappingsText
	 * 		Mappings file contents.
	 *
	 * @return Updated offset into the mappings file, skipping to the end of the line.
	 */
	private static int skipLineRest(int i, @Nonnull String mappingsText) {
		for (int len = mappingsText.length(); i < len; i++) {
			char c = mappingsText.charAt(i);
			if (c == '\r' || c == '\n') {
				break;
			}
		}
		return i;
	}

	/**
	 * @param i
	 * 		Current offset into the mappings file.
	 * @param mappingsText
	 * 		Mappings file contents.
	 *
	 * @return Updated offset into the mappings file, skipping to the next non-space character.
	 */
	private static int skipSpace(int i, @Nonnull String mappingsText) {
		for (int len = mappingsText.length(); i < len; i++) {
			char c = mappingsText.charAt(i);
			if (c != ' ') {
				break;
			}
		}
		return i;
	}

	/**
	 * @param i
	 * 		Current offset into the mappings file.
	 * @param mappingsText
	 * 		Mappings file contents.
	 *
	 * @return Updated offset into the mappings file, skipping to the end of the token.
	 *
	 * @throws InvalidMappingException
	 * 		When a tab is encountered <i>(Unexpected indentation)</i>.
	 */
	private static int readToken(int i, @Nonnull String mappingsText) throws InvalidMappingException {
		// read until next space, newline, or comment
		for (int len = mappingsText.length(); i < len; i++) {
			char c = mappingsText.charAt(i);
			if (c == '\n' || c == '\r' || c == ' ' || c == '#') {
				break;
			}
			if (c == '\t') {
				throw new InvalidMappingException("Unexpected tab character @char " + i);
			}
		}
		return i;
	}

	private static int getMaxParameterIndex(@Nullable ClassPathNode classPath,
	                                        @Nonnull String methodName, @Nonnull String methodDesc) {
		int maxParam = 100; // Ain't no way you have a legit method with more than 100 parameters.
		if (classPath != null) {
			ClassMemberPathNode memberPath = classPath.child(methodName, methodDesc);
			if (memberPath != null) {
				maxParam = memberPath.getValueAsMethod().hasStaticModifier() ? 0 : 1;
				for (Type argumentType : Type.getMethodType(methodDesc).getArgumentTypes())
					maxParam += argumentType.getSize();
			}
		}
		return maxParam;
	}

	@Nonnull
	private static String exportHierarchy(@Nonnull IntermediateMappings intermediate, @Nullable Workspace workspace) {
		// Build map of root classes to emit.
		// This way we won't naively emit inner classes as roots and instead show them as children of their outer classes.
		TreeMap<String, ExportClass> classes = new TreeMap<>();
		TreeMap<String, ExportClass> roots = new TreeMap<>();
		for (String oldClassName : intermediate.getClassesWithMappings())
			getExportClass(oldClassName, workspace, intermediate, classes, roots);

		StringBuilder sb = new StringBuilder();
		for (ExportClass root : roots.values())
			appendClass(sb, root, intermediate, 0);
		return sb.toString();
	}

	@Nonnull
	private static ExportClass getExportClass(@Nonnull String oldClassName, @Nullable Workspace workspace,
	                                          @Nonnull IntermediateMappings intermediate,
	                                          @Nonnull Map<String, ExportClass> classes,
	                                          @Nonnull Map<String, ExportClass> roots) {
		// Get existing class if created.
		ExportClass exportClass = classes.get(oldClassName);
		if (exportClass != null)
			return exportClass;

		// Create new class and add to the map.
		ClassPathNode classPath = workspace == null ? null : workspace.findClass(oldClassName);
		exportClass = new ExportClass(oldClassName, classPath);
		classes.put(oldClassName, exportClass);

		// If this is an inner class of another, try to find the outer class and add this as a child.
		ClassMapping classMapping = intermediate.getClassMapping(oldClassName);
		String outerClassName = getOuterClassName(oldClassName, classPath);
		if (outerClassName != null && oldClassName.startsWith(outerClassName + '$')) {
			ExportClass outerClass = getExportClass(outerClassName, workspace, intermediate, classes, roots);
			String childPrefix = outerClass.newName + '$';
			if (classMapping == null || classMapping.getNewName().startsWith(childPrefix)) {
				exportClass.parent = outerClass;
				exportClass.newName = classMapping == null
						? childPrefix + oldClassName.substring(outerClassName.length() + 1)
						: classMapping.getNewName();
				outerClass.children.put(oldClassName, exportClass);
				return exportClass;
			}
		}
		exportClass.newName = classMapping == null ? oldClassName : classMapping.getNewName();

		// If this is a root class (not an inner class) then add it to the root map.
		roots.put(oldClassName, exportClass);
		return exportClass;
	}

	@Nullable
	private static String getOuterClassName(@Nonnull String className, @Nullable ClassPathNode classPath) {
		// Try to get the outer class name from the class metadata, if available.
		if (classPath != null) {
			ClassInfo info = classPath.getValue();
			String outerClassName = info.getOuterClassName();
			if (outerClassName != null)
				return outerClassName;
			if (!info.isInnerClass())
				return null;
			List<String> outerClassBreadcrumbs = info.getOuterClassBreadcrumbs();
			if (!outerClassBreadcrumbs.isEmpty())
				return outerClassBreadcrumbs.getLast();
		}

		// Without class metadata, '$' is the only way to distinguish nested class names in mapping data.
		// This isn't guaranteed to be correct, but it's the best we can do at this point.
		int separator = className.lastIndexOf('$');
		if (separator > className.lastIndexOf('/'))
			return className.substring(0, separator);
		return null;
	}

	private static void appendClass(@Nonnull StringBuilder sb, @Nonnull ExportClass exportClass,
	                                @Nonnull IntermediateMappings intermediate, int indent) {
		String oldClassName = exportClass.oldName;
		ClassMapping classMapping = intermediate.getClassMapping(oldClassName);
		appendIndent(sb, indent);

		// CLASS BaseClass TargetClass
		sb.append("CLASS ");
		if (exportClass.parent == null) {
			sb.append(oldClassName);

			// Not all Enigma mappings have new names for classes.
			// These lines exist to give context to following member lines.
			if (classMapping != null)
				sb.append(' ').append(classMapping.getNewName());
		} else {
			String oldSimpleName = oldClassName.substring(exportClass.parent.oldName.length() + 1);
			sb.append(oldSimpleName);
			if (classMapping != null) {
				String newSimpleName = exportClass.newName.substring(exportClass.parent.newName.length() + 1);
				sb.append(' ').append(newSimpleName);
			}
		}
		sb.append('\n');

		// Members
		ClassPathNode classPath = exportClass.classPath;
		for (FieldMapping fieldMapping : intermediate.getClassFieldMappings(oldClassName)) {
			appendIndent(sb, indent + 1);

			// FIELD baseField targetField baseDesc
			sb.append("FIELD ")
					.append(fieldMapping.getOldName()).append(' ')
					.append(fieldMapping.getNewName()).append(' ')
					.append(fieldMapping.getDesc()).append('\n');
		}
		Map<String, List<VariableMapping>> variablesByMethod = getClassVariablesByMethod(intermediate, oldClassName);
		for (MethodMapping methodMapping : intermediate.getClassMethodMappings(oldClassName)) {
			String oldMethodName = methodMapping.getOldName();
			String methodDesc = methodMapping.getDesc();
			appendIndent(sb, indent + 1);

			// METHOD baseMethod targetMethod baseMethodDesc
			sb.append("METHOD ")
					.append(oldMethodName).append(' ')
					.append(methodMapping.getNewName()).append(' ')
					.append(methodDesc).append('\n');

			// When we emit these variables also remove them from the map.
			// Any remaining items will be from methods that were not mapped, but we still want to emit their variables.
			appendVariables(sb, classPath, oldMethodName, methodDesc,
					variablesByMethod.remove(methodKey(oldMethodName, methodDesc)), indent + 2);
		}
		for (List<VariableMapping> variables : variablesByMethod.values()) {
			if (variables.isEmpty())
				continue;
			VariableMapping firstVariable = variables.getFirst();
			String oldMethodName = firstVariable.getMethodName();
			String methodDesc = firstVariable.getMethodDesc();
			appendIndent(sb, indent + 1);
			sb.append("METHOD ")
					.append(oldMethodName).append(' ')
					.append(methodDesc).append('\n');
			appendVariables(sb, classPath, oldMethodName, methodDesc, variables, indent + 2);
		}

		// Inner classes
		for (ExportClass child : exportClass.children.values())
			appendClass(sb, child, intermediate, indent + 1);
	}

	private static void appendIndent(@Nonnull StringBuilder sb, int indent) {
		sb.append("\t".repeat(Math.max(0, indent)));
	}

	private static void appendVariables(@Nonnull StringBuilder sb, @Nullable ClassPathNode classPath,
	                                    @Nonnull String oldMethodName, @Nonnull String methodDesc,
	                                    @Nullable List<VariableMapping> variables, int indent) {
		if (variables == null || variables.isEmpty())
			return;

		// Enigma only officially supports parameters.
		// - ARG index newName
		// We can add our own special prefix (that gets ignored by the mainstream enigma tool) that allows us to export more detailed variable mappings.
		//  - VAR desc oldName newName
		int maxParam = getMaxParameterIndex(classPath, oldMethodName, methodDesc);
		for (VariableMapping variable : variables) {
			// First add args for method parameter locals.
			// These are the only locals that enigma officially supports.
			if (variable.getIndex() >= 0 && variable.getIndex() < maxParam) {
				appendIndent(sb, indent);
				sb.append("ARG ")
						.append(variable.getIndex()).append(' ')
						.append(variable.getNewName()).append('\n');
			}

			// Then add all locals for method body variables.
			// These are not officially supported by enigma,
			// but we can add our own special prefix that gets ignored by the mainstream enigma tool.
			appendIndent(sb, indent);
			sb.append("VAR ")
					.append(variable.getDesc()).append(' ')
					.append(variable.getOldName()).append(' ')
					.append(variable.getNewName()).append('\n');
		}
	}

	@Nonnull
	private static Map<String, List<VariableMapping>> getClassVariablesByMethod(@Nonnull IntermediateMappings intermediate,
	                                                                            @Nonnull String oldClassName) {
		Map<String, List<VariableMapping>> variablesByMethod = new TreeMap<>();
		for (List<VariableMapping> variables : intermediate.getVariables().values()) {
			for (VariableMapping variable : variables) {
				if (oldClassName.equals(variable.getOwnerName())) {
					variablesByMethod.computeIfAbsent(methodKey(variable.getMethodName(), variable.getMethodDesc()), key -> new ArrayList<>())
							.add(variable);
				}
			}
		}
		return variablesByMethod;
	}

	@Nonnull
	private static String methodKey(@Nonnull String methodName, @Nonnull String methodDesc) {
		// Arbitrary key for variable mapping grouping.
		return methodName + '.' + methodDesc;
	}

	@Nonnull
	private static String removeNonePackage(@Nonnull String text) {
		// None prefix is what Enigma uses to indicate that a class is in the default package.
		return text.replaceAll("(?:^|(?<=L))none/", "");
	}

	private interface MemberMappingsConsumer {
		void accept(String oldClassName, String desc, String oldName, String newName);
	}

	private static class ParseContext {
		private final Deque<Pair<String, String>> currentClass = new ArrayDeque<>();
		private MethodContext currentMethod;
	}

	private static class MethodContext {
		private final int indent;
		private final String ownerName;
		private final String methodName;
		private final String methodDesc;

		private MethodContext(int indent, @Nonnull String ownerName, @Nonnull String methodName, @Nonnull String methodDesc) {
			this.indent = indent;
			this.ownerName = ownerName;
			this.methodName = methodName;
			this.methodDesc = methodDesc;
		}
	}

	private static class MemberMappingInfo {
		private final int offset;
		private final String ownerName;
		private final String oldName;
		private final String desc;

		private MemberMappingInfo(int offset) {
			this.offset = offset;
			this.ownerName = null;
			this.oldName = null;
			this.desc = null;
		}

		private MemberMappingInfo(int offset, @Nonnull String ownerName, @Nonnull String oldName, @Nonnull String desc) {
			this.offset = offset;
			this.ownerName = ownerName;
			this.oldName = oldName;
			this.desc = desc;
		}

		private boolean isValid() {
			return ownerName != null;
		}
	}

	private static class ExportClass {
		private final Map<String, ExportClass> children = new TreeMap<>();
		private final String oldName;
		private final ClassPathNode classPath;
		private ExportClass parent;
		private String newName;

		private ExportClass(@Nonnull String oldName, @Nullable ClassPathNode classPath) {
			this.oldName = oldName;
			this.classPath = classPath;
		}
	}
}
