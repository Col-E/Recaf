package software.coley.recaf.services.mapping.format;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.Mappings;
import software.coley.recaf.services.mapping.data.ClassMapping;
import software.coley.recaf.services.mapping.data.FieldMapping;
import software.coley.recaf.services.mapping.data.MethodMapping;
import software.coley.recaf.services.mapping.data.VariableMapping;
import software.coley.recaf.util.StringUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static software.coley.recaf.util.EscapeUtil.escapeStandardAndUnicodeWhitespace;
import static software.coley.recaf.util.EscapeUtil.unescapeStandardAndUnicodeWhitespace;

/**
 * Simple mappings file implementation where the old/new names are split by a space.
 * The input format of the mappings is based on the format outlined by
 * {@link org.objectweb.asm.commons.SimpleRemapper#SimpleRemapper(int, Map)}.
 * <br>
 * Differences include:
 * <ul>
 *     <li>Support for {@code #comment} lines</li>
 *     <li>Support for unicode escape sequences ({@code \\uXXXX})</li>
 *     <li>Support for fields specified by their name <i>and descriptor</i></li>
 *     <li>Support for local variables specified by their owner signature + name/type</li>
 * </ul>
 *
 * @author Matt Coley
 * @author Wolfie / win32kbase
 */
@Dependent
public class SimpleMappings extends AbstractMappingFileFormat {
	public static final String NAME = "Simple";

	/**
	 * New simple instance.
	 */
	public SimpleMappings() {
		super(NAME, true, true);
	}

	@Nonnull
	@Override
	public IntermediateMappings parse(@Nonnull String mappingText) {
		IntermediateMappings mappings = new IntermediateMappings();
		String[] lines = StringUtil.splitNewline(mappingText);
		// # Comment
		// BaseClass TargetClass
		// BaseClass.baseField targetField
		// BaseClass.baseField baseDesc targetField
		// BaseClass.baseMethod(BaseMethodDesc) targetMethod
		// BaseClass.baseMethod(BaseMethodDesc) oldVarDesc oldVarName newVarName
		for (String line : lines) {
			// Skip comments and empty lines
			if (line.trim().startsWith("#") || line.trim().isEmpty())
				continue;
			String[] args = line.trim().split(" +");
			if (args.length < 2)
				continue;
			String oldBaseName = unescapeStandardAndUnicodeWhitespace(args[0]);
			String newName = unescapeStandardAndUnicodeWhitespace(args[args.length - 1]);
			int dot = oldBaseName.lastIndexOf('.');
			if (dot > 0) {
				// Indicates a member
				String oldClassName = oldBaseName.substring(0, dot);
				String oldIdentifier = oldBaseName.substring(dot + 1);
				int methodDescStart = oldIdentifier.lastIndexOf("(");
				if (methodDescStart > 0) {
					// Method descriptor part of ID, split it up.
					String methodName = oldIdentifier.substring(0, methodDescStart);
					String methodDesc = oldIdentifier.substring(methodDescStart);
					if (args.length >= 4) {
						// Indicates a variable mapping
						String variableDesc = unescapeStandardAndUnicodeWhitespace(args[1]);
						String variableName = unescapeStandardAndUnicodeWhitespace(args[2]);
						mappings.addVariable(oldClassName, methodName, methodDesc, variableDesc, variableName, -1, newName);
					} else if (args.length == 2) {
						mappings.addMethod(oldClassName, methodDesc, methodName, newName);
					}
				} else {
					if (args.length >= 3) {
						// Descriptor qualified field format
						String desc = unescapeStandardAndUnicodeWhitespace(args[1]);
						mappings.addField(oldClassName, desc, oldIdentifier, newName);
					} else {
						// Likely a field without linked descriptor
						mappings.addField(oldClassName, null, oldIdentifier, newName);
					}
				}
			} else {
				mappings.addClass(oldBaseName, newName);
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
			String escapedOldClassName = escapeStandardAndUnicodeWhitespace(oldClassName);
			if (classMapping != null) {
				String newClassName = classMapping.getNewName();
				// BaseClass TargetClass
				sb.append(escapedOldClassName).append(' ').append(newClassName).append("\n");
			}
			for (FieldMapping fieldMapping : intermediate.getClassFieldMappings(oldClassName)) {
				String oldFieldName = escapeStandardAndUnicodeWhitespace(fieldMapping.getOldName());
				String newFieldName = escapeStandardAndUnicodeWhitespace(fieldMapping.getNewName());
				String fieldDesc = escapeStandardAndUnicodeWhitespace(fieldMapping.getDesc());
				if (fieldDesc != null) {
					// BaseClass.baseField baseDesc targetField
					sb.append(escapedOldClassName).append('.').append(oldFieldName)
							.append(' ').append(fieldDesc)
							.append(' ').append(newFieldName).append("\n");
				} else {
					// BaseClass.baseField targetField
					sb.append(escapedOldClassName).append('.').append(oldFieldName)
							.append(' ').append(newFieldName).append("\n");
				}
			}
			for (MethodMapping methodMapping : intermediate.getClassMethodMappings(oldClassName)) {
				String oldMethodName = escapeStandardAndUnicodeWhitespace(methodMapping.getOldName());
				String newMethodName = escapeStandardAndUnicodeWhitespace(methodMapping.getNewName());
				String methodDesc = escapeStandardAndUnicodeWhitespace(methodMapping.getDesc());
				// BaseClass.baseMethod(BaseMethodDesc) targetMethod
				sb.append(escapedOldClassName).append('.').append(oldMethodName)
						.append(methodDesc)
						.append(' ').append(newMethodName).append("\n");
			}
			for (VariableMapping variableMapping : getClassVariableMappings(intermediate, oldClassName)) {
				String oldVariableName = variableMapping.getOldName();
				String variableDesc = variableMapping.getDesc();
				if (oldVariableName == null || variableDesc == null)
					continue;

				String oldMethodName = escapeStandardAndUnicodeWhitespace(variableMapping.getMethodName());
				String methodDesc = escapeStandardAndUnicodeWhitespace(variableMapping.getMethodDesc());
				String escapedVariableDesc = escapeStandardAndUnicodeWhitespace(variableDesc);
				String escapedOldVariableName = escapeStandardAndUnicodeWhitespace(oldVariableName);
				String newVariableName = escapeStandardAndUnicodeWhitespace(variableMapping.getNewName());
				// BaseClass.baseMethod(BaseMethodDesc) oldVarDesc oldVarName newVarName
				sb.append(escapedOldClassName).append('.').append(oldMethodName)
						.append(methodDesc)
						.append(' ').append(escapedVariableDesc)
						.append(' ').append(escapedOldVariableName)
						.append(' ').append(newVariableName).append("\n");
			}
		}
		return sb.toString().trim();
	}

	@Nonnull
	private static List<VariableMapping> getClassVariableMappings(@Nonnull IntermediateMappings intermediate,
	                                                              @Nonnull String oldClassName) {
		List<VariableMapping> classVariables = new ArrayList<>();
		for (List<VariableMapping> methodVariables : intermediate.getVariables().values()) {
			for (VariableMapping variableMapping : methodVariables) {
				if (oldClassName.equals(variableMapping.getOwnerName()))
					classVariables.add(variableMapping);
			}
		}
		classVariables.sort(Comparator.comparing(VariableMapping::getMethodName)
				.thenComparing(VariableMapping::getMethodDesc)
				.thenComparing(variable -> Objects.toString(variable.getDesc(), ""))
				.thenComparing(variable -> Objects.toString(variable.getOldName(), ""))
				.thenComparing(VariableMapping::getNewName));
		return classVariables;
	}
}
