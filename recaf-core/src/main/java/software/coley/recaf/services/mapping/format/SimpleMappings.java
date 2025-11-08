package software.coley.recaf.services.mapping.format;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.Mappings;
import software.coley.recaf.services.mapping.data.ClassMapping;
import software.coley.recaf.services.mapping.data.FieldMapping;
import software.coley.recaf.services.mapping.data.MethodMapping;
import software.coley.recaf.util.StringUtil;

import java.util.Map;

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
		for (String line : lines) {
			// Skip comments and empty lines
			if (line.trim().startsWith("#") || line.trim().isEmpty())
				continue;
			String[] args = line.split(" ");
			String oldBaseName = unescapeStandardAndUnicodeWhitespace(args[0]);
			if (args.length >= 3) {
				// Descriptor qualified field format
				String desc = unescapeStandardAndUnicodeWhitespace(args[1]);
				String targetName = unescapeStandardAndUnicodeWhitespace(args[2]);
				int dot = oldBaseName.lastIndexOf('.');
				String oldClassName = oldBaseName.substring(0, dot);
				String oldFieldName = oldBaseName.substring(dot + 1);
				mappings.addField(oldClassName, desc, oldFieldName, targetName);
			} else {
				String newName = unescapeStandardAndUnicodeWhitespace(args[1]);
				int dot = oldBaseName.lastIndexOf('.');
				if (dot > 0) {
					// Indicates a member
					String oldClassName = oldBaseName.substring(0, dot);
					String oldIdentifier = oldBaseName.substring(dot + 1);
					int methodDescStart = oldIdentifier.lastIndexOf("(");
					if (methodDescStart > 0) {
						// Method descriptor part of ID, split it up
						String methodName = oldIdentifier.substring(0, methodDescStart);
						String methodDesc = oldIdentifier.substring(methodDescStart);
						mappings.addMethod(oldClassName, methodDesc, methodName, newName);
					} else {
						// Likely a field without linked descriptor
						mappings.addField(oldClassName, null, oldIdentifier, newName);
					}
				} else {
					mappings.addClass(oldBaseName, newName);
				}
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
		}
		return sb.toString();
	}
}
