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
import software.coley.recaf.util.StringUtil;

import java.util.Stack;

/**
 * Enigma mappings file implementation.
 * <p>
 * Specification: <a href="https://wiki.fabricmc.net/documentation:enigma_mappings">enigma_mappings</a>
 *
 * @author Matt Coley
 */
@Dependent
public class EnigmaMappings extends AbstractMappingFileFormat {
	public static final String NAME = "Enigma";
	private static final String FAIL = "Invalid Enigma mappings, ";
	private final Logger logger = Logging.get(EnigmaMappings.class);

	/**
	 * New enigma instance.
	 */
	public EnigmaMappings() {
		super(NAME, true, true);
	}

	@Nonnull
	@Override
	public IntermediateMappings parse(@Nonnull String mappingText) {
		IntermediateMappings mappings = new IntermediateMappings();
		String[] lines = StringUtil.splitNewline(mappingText);
		// COMMENT comment
		// CLASS BaseClass TargetClass
		//     FIELD baseField targetField baseDesc
		//     METHOD baseMethod targetMethod baseMethodDesc
		//         ARG baseArg targetArg
		Stack<String> currentClass = new Stack<>();
		for (int i = 0; i < lines.length; i++) {
			String lineStr = lines[i];
			String lineStrTrim = lineStr.trim();
			if (lineStrTrim.isBlank())
				continue;
			int strIndent = lineStr.indexOf(lineStrTrim) + 1;
			String[] args = lineStrTrim.split(" ");
			if (args.length == 0)
				continue;
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
							mappings.addClass(currentClass.peek(), renamedClass);
						}
						break;
					case "FIELD":
						// Check if no longer within inner-class scope
						if (strIndent < currentClass.size())
							currentClass.pop();

						// Parse field
						if (currentClass.empty())
							throw new IllegalArgumentException(FAIL + "could not map field, no class context");

						// Skip if there aren't enough arguments to pull the necessary items
						if (args.length < 4)
							continue;

						String currentField = removeNonePackage(args[1]);
						String renamedField = removeNonePackage(args[2]);
						String currentFieldDesc = removeNonePackage(args[3]);
						mappings.addField(currentClass.peek(), currentFieldDesc, currentField, renamedField);
						break;
					case "METHOD":
						// Check if no longer within inner-class scope
						if (strIndent < currentClass.size())
							currentClass.pop();

						// Parse method
						if (currentClass.empty())
							throw new IllegalArgumentException(FAIL + "could not map method, no class context");

						// Skip if there aren't enough arguments to pull the necessary items
						if (args.length < 4)
							continue;

						// Skip constructors/initializers
						String currentMethod = args[1];
						if (currentMethod.startsWith("<"))
							continue;

						// Not all methods need to be renamed if they have child arg elements that are renamed
						if (args.length >= 4) {
							String renamedMethod = args[2];
							String methodType = args[3];
							mappings.addMethod(currentClass.peek(), methodType, currentMethod, renamedMethod);
						}
						break;
					case "COMMENT":
					case "ARG":
						// Do nothing, mapper does not support comments & arg names
						break;
					default:
						logger.trace("Unknown Engima mappings line type: \"{}\" @line {}", type, i);
						break;
				}
			} catch (IndexOutOfBoundsException ex) {
				throw new IllegalArgumentException(FAIL + "failed parsing line " + i, ex);
			}
		}
		return mappings;
	}

	@Override
	public String exportText(@Nonnull Mappings mappings) {
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

	private static String removeNonePackage(String text) {
		return text.replaceAll("(?:^|(?<=L))none/", "");
	}
}
