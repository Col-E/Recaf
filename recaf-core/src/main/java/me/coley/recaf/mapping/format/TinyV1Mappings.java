package me.coley.recaf.mapping.format;

import me.coley.recaf.mapping.MappingsAdapter;
import me.coley.recaf.mapping.data.ClassMapping;
import me.coley.recaf.mapping.data.FieldMapping;
import me.coley.recaf.mapping.data.MethodMapping;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

/**
 * Tiny-V1 mappings file implementation.
 *
 * @author Matt Coley
 * @author Wolfie / win32kbase
 */
public class TinyV1Mappings extends MappingsAdapter {
	private final Logger logger = Logging.get(TinyV1Mappings.class);

	/**
	 * New tiny v1 instance.
	 */
	public TinyV1Mappings() {
		super("Tiny V1", true, true);
	}

	@Override
	public boolean supportsExportText() {
		return true;
	}

	@Override
	public void parse(String mappingText) {
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
						addClass(oldClass, newClass);
						break;
					}
					case "FIELD": {
						String oldOwner = args[1];
						String oldDesc = args[2];
						String oldName = args[3];
						String newName = args[4];
						addField(oldOwner, oldName, oldDesc, newName);
						break;
					}
					case "METHOD": {
						String oldOwner = args[1];
						String oldDesc = args[2];
						String oldName = args[3];
						String newName = args[4];
						addMethod(oldOwner, oldName, oldDesc, newName);
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
	}

	@Override
	public String exportText() {
		StringBuilder sb = new StringBuilder("v1\tintermediary\tnamed\n");
		IntermediateMappings intermediate = exportIntermediate();
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
