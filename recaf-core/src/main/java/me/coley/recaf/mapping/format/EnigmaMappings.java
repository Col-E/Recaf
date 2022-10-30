package me.coley.recaf.mapping.format;

import me.coley.recaf.mapping.MappingsAdapter;
import me.coley.recaf.mapping.data.ClassMapping;
import me.coley.recaf.mapping.data.FieldMapping;
import me.coley.recaf.mapping.data.MethodMapping;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.util.Stack;

/**
 * Enigma mappings file implementation.
 *
 * @author Matt Coley
 */
public class EnigmaMappings extends MappingsAdapter {
	private static final String FAIL = "Invalid Enigma mappings, ";
	private final Logger logger = Logging.get(EnigmaMappings.class);

	/**
	 * New enigma instance.
	 */
	public EnigmaMappings() {
		super("Enigma", true, true);
	}

	@Override
	public boolean supportsExportText() {
		return true;
	}

	@Override
	public void parse(String mappingText) {
		String[] lines = StringUtil.splitNewline(mappingText);
		// COMMENT comment
		// CLASS BaseClass TargetClass
		//     FIELD baseField targetField baseDesc
		//     METHOD baseMethod targetMethod baseMethodDesc
		//         ARG baseArg targetArg
		int line = 0;
		Stack<String> currentClass = new Stack<>();
		for (String lineStr : lines) {
			line++;
			String lineStrTrim = lineStr.trim();
			int strIndent = lineStr.indexOf(lineStrTrim) + 1;
			String[] args = lineStrTrim.split(" ");
			String type = args[0];
			try {
				switch (type) {
					case "CLASS":
						if (lineStr.matches("\\s+.+")) {
							// Check for indentation, implies the class is an inner
							currentClass.add(removeNonePackage(args[1]));
						} else {
							// Root level class
							currentClass.clear();
							currentClass.add(removeNonePackage(args[1]));
						}
						// Not all classes need to be renamed if they have child elements that are renamed
						if (args.length >= 3) {
							String renamedClass = removeNonePackage(args[2]);
							addClass(currentClass.peek(), renamedClass);
						}
						break;
					case "FIELD":
						// Check if no longer within inner-class scope
						if (strIndent < currentClass.size()) {
							currentClass.pop();
						}
						// Parse field
						if (currentClass.empty())
							throw new IllegalArgumentException(FAIL + "could not map field, no class context");
						String currentField = removeNonePackage(args[1]);
						String renamedField = removeNonePackage(args[2]);
						String currentFieldDesc = removeNonePackage(args[3]);
						addField(currentClass.peek(), currentField, currentFieldDesc, renamedField);
						break;
					case "METHOD":
						// Check if no longer within inner-class scope
						if (strIndent < currentClass.size()) {
							currentClass.pop();
						}
						// Parse method
						if (currentClass.empty())
							throw new IllegalArgumentException(FAIL + "could not map method, no class context");
						String currentMethod = args[1];
						if (currentMethod.equals("<init>"))
							continue;
						// Not all methods need to be renamed if they have child arg elements that are renamed
						if (args.length >= 4) {
							String renamedMethod = args[2];
							String methodType = args[3];
							addMethod(currentClass.peek(), currentMethod, methodType, renamedMethod);
						}
						break;
					case "COMMENT":
					case "ARG":
						// Do nothing, mapper does not support comments & arg names
						break;
					default:
						logger.trace("Unknown Engima mappings line type: \"{}\" @line {}", type, line);
						break;
				}
			} catch (IndexOutOfBoundsException ex) {
				throw new IllegalArgumentException(FAIL + "failed parsing line " + line, ex);
			}
		}
	}

	@Override
	public String exportText() {
		StringBuilder sb = new StringBuilder();
		IntermediateMappings intermediate = exportIntermediate();
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

	private static String removeNonePackage(String text) {
		return text.replaceAll("(?:^|(?<=L))none/", "");
	}
}
