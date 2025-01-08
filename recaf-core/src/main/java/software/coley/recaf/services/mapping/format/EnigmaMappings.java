package software.coley.recaf.services.mapping.format;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.Mappings;
import software.coley.recaf.services.mapping.data.ClassMapping;
import software.coley.recaf.services.mapping.data.FieldMapping;
import software.coley.recaf.services.mapping.data.MethodMapping;

import java.util.ArrayDeque;
import java.util.Deque;

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
		ParserState state = new ParserState();
		for (int i = 0; i < mappingsText.length(); i++) {
			char c = mappingsText.charAt(i);
			if (c != '\n' && c != '\r' && c != '#' && c != ' ') {
				if (state.phase == PHASE_IGNORE_LINE) continue; // inside # or ignored type
				if (state.phase == PHASE_FIND_TYPE) {
					if (c == '\t') { // read tab
						state.indent++;
						continue;
					}

					// If indent is lower than current class depth, pop
					while (state.indent < state.currentClass.size()) {
						state.currentClass.pop();
					}
				}

				// start of new token
				if (state.start == -1) {
					state.start = i;
				}
			} else { // newline, #, <space>
				boolean isSpace = c == ' ';
				if (isSpace || state.phase != PHASE_IGNORE_LINE && state.phase < PHASE_TYPE_FLAG_FINISH) {
					// finished reading a token
					handleToken(mappingsText, state, i);
				}
				if (isSpace) continue; // continue parsing next token

				// newline or comment -> current line finished
				writeCurrentMapping(state, mappings);

				if (c == '#') {
					state.phase = 0; // skip
					continue;
				}

				state.line++;

				// skip two-char newline to not count 1 new line twice
				if (c == '\r' && mappingsText.length() > i + 1 && mappingsText.charAt(i + 1) == '\n') {
					i++;
				}

				// reset values for next line
				state.phase = PHASE_FIND_TYPE;
				state.indent = 0;
				state.start = -1;
				state.typeArgs[0] = null;
				state.typeArgs[1] = null;
				state.typeArgs[2] = null;
			}
		}

		// handle and/or write last token if applicable
		if (state.phase > PHASE_FIND_TYPE) {
			if (state.phase < PHASE_TYPE_FLAG_FINISH) {
				handleToken(mappingsText, state, mappingsText.length());
			}
			writeCurrentMapping(state, mappings);
		}
		return mappings;
	}

	private static void writeCurrentMapping(@Nonnull ParserState state, @Nonnull IntermediateMappings mappings) throws InvalidMappingException {
		switch (state.phase) {
			case PHASE_TYPE_CLASS + PHASE_TYPE_FLAG_FINISH:
				if (state.typeArgsIndex == 2) {
					String cls = state.currentClass.peek();
					if (cls == null)
						throw new InvalidMappingException(FAIL + "cannot peek current class context when finishing class section");
					mappings.addClass(cls, state.typeArgs[1]);
				}
				break;
			case PHASE_TYPE_FIELD + PHASE_TYPE_FLAG_FINISH:
				if (state.typeArgsIndex == 3) {
					String cls = state.currentClass.peek();
					if (cls == null)
						throw new InvalidMappingException(FAIL + "cannot peek current class context when finishing field section");
					mappings.addField(cls, state.typeArgs[2], state.typeArgs[0], state.typeArgs[1]);
				}
				break;
			case PHASE_TYPE_METHOD + PHASE_TYPE_FLAG_FINISH:
				if (state.typeArgsIndex == 3) {
					String cls = state.currentClass.peek();
					if (cls == null)
						throw new InvalidMappingException(FAIL + "cannot peek current class context when finishing method section");
					mappings.addMethod(cls, state.typeArgs[2], state.typeArgs[0], state.typeArgs[1]);
				}
				break;
		}
	}

	private static void handleToken(@Nonnull String mappingText, @Nonnull ParserState state, int i) throws InvalidMappingException {
		switch (state.phase) {
			case PHASE_FIND_TYPE -> {
				state.typeArgsIndex = 0;
				String typeStr = mappingText.substring(state.start, i);
				switch (typeStr) {
					case "CLASS" -> state.phase = 2;
					case "FIELD" -> {
						if (state.currentClass.isEmpty()) {
							throw new InvalidMappingException(FAIL + "could not map field, no class context @line " + state.line + " @char " + i);
						}
						state.phase = PHASE_TYPE_FIELD;
					}
					case "METHOD" -> {
						if (state.currentClass.isEmpty()) {
							throw new InvalidMappingException(FAIL + "could not map method, no class context @line " + state.line + " @char " + i);
						}
						state.phase = PHASE_TYPE_METHOD;
					}
					case "ARG", "COMMENT" -> state.phase = PHASE_IGNORE_LINE;
					default -> {
						LOGGER.trace("Unknown Engima mappings line type: \"{}\" @line {} @char {}", state.phase, state.line, i);
						state.phase = PHASE_IGNORE_LINE;
					}
				}
			}
			case PHASE_TYPE_CLASS -> {
				// <class-section> ::= <class-section-indentation> 'CLASS' <space> <class-name-a> <class-name-b>
				//                     <formatted-access-modifier> <eol> <class-sub-sections>
				// <formatted-access-modifier> ::= '' | <space> 'ACC:' <access-modifier>

				// read class-name-a, class-name-b (optional)
				// when finished, add FINISH_FLAG to type
				String currArg = removeNonePackage(mappingText.substring(state.start, i));
				switch (state.typeArgsIndex) {
					case 0 -> { // class-name-a
						if (!state.currentClass.isEmpty()) {
							StringBuilder sb = new StringBuilder();
							for (String clazz : state.currentClass) {
								sb.append(clazz).append('$');
							}
							currArg = sb.append(currArg).toString();
						}
						state.currentClass.push(currArg);
						state.typeArgs[state.typeArgsIndex++] = currArg;
					}
					case 1 -> { // class-name-b (optional) | skip access modifier
						if (currArg.isEmpty() || "-".equals(currArg) || currArg.startsWith("ACC:")) {
							state.phase += PHASE_TYPE_FLAG_FINISH;
							break;
						}
						state.typeArgs[state.typeArgsIndex++] = currArg;
						state.phase += PHASE_TYPE_FLAG_FINISH;
					}
				}
			}
			case PHASE_TYPE_FIELD -> {
				// <field-section> ::= <class-section-indentation> <tab> 'FIELD'<space> <field-name-a> <field-name-b>
				//                     <formatted-access-modifier> <field-desc-a> <eol> <field-sub-sections>
				// <formatted-access-modifier> ::= '' | <space> 'ACC:' <access-modifier>

				// read field-name-a, field-name-b (optional), skip access modifier, read field-desc-a
				// when optional, need to check the read thing is not the next one
				// when finished, add FINISH_FLAG to type
				String currArg = mappingText.substring(state.start, i);
				switch (state.typeArgsIndex) {
					case 0: // field-name-a
						state.typeArgs[state.typeArgsIndex++] = currArg;
						break;
					case 1: // field-name-b (optional)
						if (currArg.isEmpty() || "-".equals(currArg) || currArg.startsWith("ACC:")) {
							state.phase += PHASE_TYPE_FLAG_FINISH;
							break;
						}
						state.typeArgs[state.typeArgsIndex++] = currArg;
						break;
					case 2: // access-modifier (skip) | field-desc-a
						if (currArg.isEmpty() || currArg.startsWith("ACC:")) {
							break;
						}
						state.typeArgs[state.typeArgsIndex++] = currArg;
						state.phase += PHASE_TYPE_FLAG_FINISH;
						break;
				}
			}
			case PHASE_TYPE_METHOD -> {
				// <method-section> ::= <class-section-indentation> <tab> 'METHOD' <space> <method-name-a> <method-name-b>
				//                      <formatted-access-modifier> <method-desc-a> <eol> <method-sub-sections>
				// <formatted-access-modifier> ::= '' | <space> 'ACC:' <access-modifier>

				// read method-name-a, method-name-b (optional), skip access modifier, read method-desc-a
				// when finished, add FINISH_FLAG to type
				String currArg = mappingText.substring(state.start, i);
				switch (state.typeArgsIndex) {
					case 0: // method-name-a
						state.typeArgs[state.typeArgsIndex++] = currArg;
						break;
					case 1: // method-name-b (optional)
						if (currArg.isEmpty() || "-".equals(currArg) || currArg.startsWith("ACC:")) {
							state.phase += PHASE_TYPE_FLAG_FINISH;
							break;
						}
						state.typeArgs[state.typeArgsIndex++] = currArg;
						break;
					case 2: // access-modifier (skip) | method-desc-a
						if (currArg.isEmpty() || currArg.startsWith("ACC:")) {
							break;
						}
						state.typeArgs[state.typeArgsIndex++] = currArg;
						state.phase += PHASE_TYPE_FLAG_FINISH;
						break;
				}
			}
			default -> throw new InvalidMappingException("Unexpected value: " + state.phase);
		}
		state.start = -1;
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

	private static final class ParserState {
		private byte indent = 0;
		private byte phase = PHASE_FIND_TYPE;
		private byte typeArgsIndex = 0;
		/**
		 * {@code -1} indicates search for token start
		 */
		private int start = -1;
		private int line = 1;
		private final String[] typeArgs = new String[3];
		private final Deque<String> currentClass = new ArrayDeque<>();
	}
}
