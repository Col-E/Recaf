package software.coley.recaf.services.mapping.format;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import org.slf4j.Logger;
import software.coley.collections.tuple.Pair;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.Mappings;
import software.coley.recaf.services.mapping.data.ClassMapping;
import software.coley.recaf.services.mapping.data.FieldMapping;
import software.coley.recaf.services.mapping.data.MethodMapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
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
		//         ARG baseArg targetArg #ignored
		//     CLASS 1 InnerClass
		//         FIELD innerField targetField innerDesc
		IntermediateMappings mappings = new IntermediateMappings();
		Deque<Pair<String, String>> currentClass = new ArrayDeque<>();

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
			i = handleLine(line, indent, i, mappingsText, currentClass, mappings);

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
	 * @param currentClass
	 * 		Deque of the current 'context' <i>(what class are we building mappings for)</i>.
	 * @param mappings
	 * 		Output mappings.
	 *
	 * @return Updated offset into the mappings file.
	 *
	 * @throws InvalidMappingException
	 * 		When reading the mappings encounters any failure.
	 */
	private static int handleLine(int line, int indent, int i, @Nonnull String mappingsText, @Nonnull Deque<Pair<String, String>> currentClass,
	                              @Nonnull IntermediateMappings mappings) throws InvalidMappingException {
		// read next token
		String lineType = mappingsText.substring(i = skipSpace(i, mappingsText), i = readToken(i, mappingsText));
		switch (lineType) {
			case "CLASS" -> {
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
			case "FIELD" ->
					i = handleClassMemberMapping(line, indent, i, mappingsText, currentClass, "FIELD", mappings::addField);
			case "METHOD" ->
					i = handleClassMemberMapping(line, indent, i, mappingsText, currentClass, "METHOD", mappings::addMethod);
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
	 * @return Updated offset into the mappings file.
	 *
	 * @throws InvalidMappingException
	 * 		When reading the mappings encounters any failure.
	 */
	private static int handleClassMemberMapping(int line, int indent, int i, @Nonnull String mappingsText, @Nonnull Deque<Pair<String, String>> currentClass,
	                                            @Nonnull String type, @Nonnull MemberMappingsConsumer consumer) throws InvalidMappingException {
		// <name-a> <name-b> <formatted-access-modifier> <desc> <eol>
		// <name-b> = '' | '-' | <space> <name>
		updateIndent(currentClass, indent, () -> FAIL + type + " indent level " + indent + " too deep (expected max. "
				+ currentClass.size() + ", " + currentClass + ") @line " + line + " @char ", i);
		if (currentClass.isEmpty()) {
			throw new InvalidMappingException(FAIL + type + " without class context @line " + line + " @char " + i);
		}

		String nameA = mappingsText.substring(i = skipSpace(i, mappingsText), i = readToken(i, mappingsText));
		String nameB = mappingsText.substring(i = skipSpace(i, mappingsText), i = readToken(i, mappingsText));

		// If can have mapping
		if (!nameB.isEmpty() && !"-".equals(nameB) && !nameB.startsWith("ACC:")) {
			String desc = mappingsText.substring(i = skipSpace(i, mappingsText), i = readToken(i, mappingsText));
			if (desc.startsWith("ACC:")) { // skip optional access modifier
				desc = mappingsText.substring(i = skipSpace(i, mappingsText), i = readToken(i, mappingsText));
			}
			if (desc.isEmpty()) {
				// no desc found = line contained only one name and optional access modifier = no mapping
				return i;
			}
			assert currentClass.peek() != null; // checked above
			consumer.accept(currentClass.peek().getLeft(), desc, nameA, nameB);
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

	@Override
	public String exportText(@Nonnull Mappings mappings) {
		//TODO: Fix inner class handling
		// - Currently we export inner classes as top-level classes
		// - We should match the spec and have inner-classes indented beneath their outer classes

		StringBuilder sb = new StringBuilder();
		IntermediateMappings intermediate = mappings.exportIntermediate();
		for (String oldClassName : intermediate.getClassesWithMappings()) {
			ClassMapping classMapping = intermediate.getClassMapping(oldClassName);
			if (classMapping != null) {
				String newClassName = classMapping.getNewName();
				// CLASS BaseClass TargetClass
				sb.append("CLASS ")
						.append(oldClassName).append(' ')
						.append(newClassName).append("\n");
			} else {
				// Not mapped, but need to include for context for following members
				sb.append("CLASS ")
						.append(oldClassName).append("\n");
			}
			for (FieldMapping fieldMapping : intermediate.getClassFieldMappings(oldClassName)) {
				String oldFieldName = fieldMapping.getOldName();
				String newFieldName = fieldMapping.getNewName();
				String fieldDesc = fieldMapping.getDesc();
				// FIELD baseField targetField baseDesc
				sb.append("\tFIELD ")
						.append(oldFieldName).append(' ')
						.append(newFieldName).append(' ')
						.append(fieldDesc).append("\n");
			}
			for (MethodMapping methodMapping : intermediate.getClassMethodMappings(oldClassName)) {
				String oldMethodName = methodMapping.getOldName();
				String newMethodName = methodMapping.getNewName();
				String methodDesc = methodMapping.getDesc();
				// METHOD baseMethod targetMethod baseMethodDesc
				sb.append("\tMETHOD ")
						.append(oldMethodName).append(' ')
						.append(newMethodName).append(' ')
						.append(methodDesc).append("\n");
			}
		}
		return sb.toString();
	}

	@Nonnull
	private static String removeNonePackage(@Nonnull String text) {
		return text.replaceAll("(?:^|(?<=L))none/", "");
	}

	private interface MemberMappingsConsumer {
		void accept(String oldClassName, String desc, String oldName, String newName);
	}
}
