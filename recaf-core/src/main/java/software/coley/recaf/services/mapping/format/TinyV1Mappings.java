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

/**
 * Tiny-V1 mappings file implementation.
 *
 * @author Matt Coley
 * @author Wolfie / win32kbase
 */
@Dependent
public class TinyV1Mappings extends AbstractMappingFileFormat {
	public static final String NAME = "Tiny-V1";
	private final Logger logger = Logging.get(TinyV1Mappings.class);

	/**
	 * New tiny v1 instance.
	 */
	public TinyV1Mappings() {
		super(NAME, true, true);
	}

	@Override
	public IntermediateMappings parse(@Nonnull String mappingText) {
		IntermediateMappings mappings = new IntermediateMappings();
		String[] lines = StringUtil.splitNewline(mappingText);
		int lineNum = 0;
		for (String line : lines) {
			lineNum++;
			// Skip initial header
			if (line.startsWith("v1\t"))
				continue;
			String[] args = line.trim().split("\t");
			String type = args[0];
			try {
				switch (type) {
					case "CLASS": {
						String oldClass = args[1];
						String newClass = args[2];
						mappings.addClass(oldClass, newClass);
						break;
					}
					case "FIELD": {
						String oldOwner = args[1];
						String oldDesc = args[2];
						String oldName = args[3];
						String newName = args[4];
						mappings.addField(oldOwner, oldDesc, oldName, newName);
						break;
					}
					case "METHOD": {
						String oldOwner = args[1];
						String oldDesc = args[2];
						String oldName = args[3];
						String newName = args[4];
						mappings.addMethod(oldOwner, oldDesc, oldName, newName);
						break;
					}
					default: {
						logger.error("Failed to parse mapping type {} at line {}.", type, lineNum);
						break;
					}
				}
			} catch (IndexOutOfBoundsException e) {
				logger.error("Failed parsing line {}.", lineNum);
				break;
			}
		}
		return mappings;
	}

	@Override
	public String exportText(Mappings mappings) {
		StringBuilder sb = new StringBuilder("v1\tintermediary\tnamed\n");
		IntermediateMappings intermediate = mappings.exportIntermediate();
		for (String oldClassName : intermediate.getClassesWithMappings()) {
			ClassMapping classMapping = intermediate.getClassMapping(oldClassName);
			if (classMapping != null) {
				String newClassName = classMapping.getNewName();
				// CLASS BaseClass TargetClass
				sb.append("CLASS\t")
						.append(oldClassName).append('\t')
						.append(newClassName).append("\n");
			}
			for (FieldMapping fieldMapping : intermediate.getClassFieldMappings(oldClassName)) {
				String oldFieldDesc = fieldMapping.getDesc();
				String oldFieldName = fieldMapping.getOldName();
				String newFieldName = fieldMapping.getNewName();
				// FIELD BaseClass baseField targetField
				sb.append("FIELD\t").append(oldClassName).append('\t')
						.append(oldFieldName).append('\t')
						.append(oldFieldDesc).append('\t')
						.append(newFieldName).append("\n");
			}
			for (MethodMapping methodMapping : intermediate.getClassMethodMappings(oldClassName)) {
				String oldMethodName = methodMapping.getOldName();
				String newMethodName = methodMapping.getNewName();
				String methodDesc = methodMapping.getDesc();
				// METHOD BaseClass baseMethod (BaseMethodDesc) targetMethod
				sb.append("METHOD\t")
						.append(oldClassName).append('\t')
						.append(methodDesc).append('\t')
						.append(oldMethodName).append('\t')
						.append(newMethodName).append("\n");
			}
		}
		return sb.toString();
	}
}
