package me.coley.recaf.mapping.format;

import me.coley.recaf.mapping.MappingsAdapter;
import me.coley.recaf.mapping.RemapperImpl;
import me.coley.recaf.mapping.data.ClassMapping;
import me.coley.recaf.mapping.data.FieldMapping;
import me.coley.recaf.mapping.data.MethodMapping;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.logging.Logging;
import org.objectweb.asm.commons.Remapper;
import org.slf4j.Logger;

/**
 * The MCP SRG format.
 *
 * @author Matt Coley
 */
public class SrgMappings extends MappingsAdapter {
	private final Logger logger = Logging.get(TinyV1Mappings.class);

	/**
	 * New SRG instance.
	 */
	public SrgMappings() {
		super("SRG", false, false);
	}

	@Override
	public boolean supportsExportText() {
		return true;
	}

	@Override
	public void parse(String mappingText) {
		String[] lines = StringUtil.splitNewline(mappingText);
		int line = 0;
		for (String lineStr : lines) {
			line++;
			String[] args = lineStr.trim().split(" ");
			String type = args[0];
			try {
				switch (type) {
					case "PK:":
						// Ignore package entries
						break;
					case "CL:":
						String obfClass = args[1];
						String renamedClass = args[2];
						addClass(obfClass, renamedClass);
						break;
					case "FD:": {
						String obfKey = args[1];
						int splitPos = obfKey.lastIndexOf('/');
						String obfOwner = obfKey.substring(0, splitPos);
						String obfName = obfKey.substring(splitPos + 1);
						String renamedKey = args[2];
						splitPos = renamedKey.lastIndexOf('/');
						String renamedName = renamedKey.substring(splitPos + 1);
						addField(obfOwner, obfName, renamedName);
						break;
					}
					case "MD:": {
						String obfKey = args[1];
						int splitPos = obfKey.lastIndexOf('/');
						String obfOwner = obfKey.substring(0, splitPos);
						String obfName = obfKey.substring(splitPos + 1);
						String obfDesc = args[2];
						String renamedKey = args[3];
						splitPos = renamedKey.lastIndexOf('/');
						String renamedName = renamedKey.substring(splitPos + 1);
						addMethod(obfOwner, obfName, obfDesc, renamedName);
						break;
					}
					default:
						logger.trace("Unknown SRG mappings line type: \"{}\" @line {}", type, line);
						break;
				}
			} catch (IndexOutOfBoundsException ex) {
				throw new IllegalArgumentException("Failed parsing line " + line, ex);
			}
		}
	}

	@Override
	public String exportText() {
		StringBuilder sb = new StringBuilder();
		Remapper remapper = new RemapperImpl(this);
		IntermediateMappings intermediate = exportIntermediate();
		for (String oldClassName : intermediate.getClassesWithMappings()) {
			ClassMapping classMapping = intermediate.getClassMapping(oldClassName);
			if (classMapping != null) {
				String newClassName = classMapping.getNewName();
				// CL: BaseClass TargetClass
				sb.append("CL: ").append(oldClassName).append(' ')
						.append(newClassName).append("\n");
			}
			String newClassName = classMapping == null ? oldClassName : classMapping.getNewName();
			for (FieldMapping fieldMapping : intermediate.getClassFieldMappings(oldClassName)) {
				String oldFieldName = fieldMapping.getOldName();
				String newFieldName = fieldMapping.getNewName();
				// FD: BaseClass/baseField TargetClass/targetField
				sb.append("FD: ")
						.append(oldClassName).append('/').append(oldFieldName)
						.append(' ')
						.append(newClassName).append('/').append(newFieldName).append("\n");
			}
			for (MethodMapping methodMapping : intermediate.getClassMethodMappings(oldClassName)) {
				String oldMethodName = methodMapping.getOldName();
				String newMethodName = methodMapping.getNewName();
				String methodDesc = methodMapping.getDesc();
				String mappedDesc = remapper.mapDesc(methodDesc);
				// MD: BaseClass/baseMethod baseDesc TargetClass/targetMethod targetDesc
				sb.append("MD: ")
						.append(oldClassName).append('/').append(oldMethodName)
						.append(' ')
						.append(methodDesc)
						.append(' ')
						.append(newClassName).append('/').append(newMethodName)
						.append(' ')
						.append(mappedDesc).append('\n');
			}
		}
		return sb.toString();
	}
}
