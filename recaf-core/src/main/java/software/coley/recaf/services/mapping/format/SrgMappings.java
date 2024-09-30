package software.coley.recaf.services.mapping.format;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import org.objectweb.asm.commons.Remapper;
import org.slf4j.Logger;
import software.coley.collections.tuple.Pair;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.mapping.BasicMappingsRemapper;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.Mappings;
import software.coley.recaf.services.mapping.data.ClassMapping;
import software.coley.recaf.services.mapping.data.FieldMapping;
import software.coley.recaf.services.mapping.data.MethodMapping;
import software.coley.recaf.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * The MCP SRG format.
 *
 * @author Matt Coley
 */
@Dependent
public class SrgMappings extends AbstractMappingFileFormat {
	public static final String NAME = "SRG";
	private final Logger logger = Logging.get(TinyV1Mappings.class);

	/**
	 * New SRG instance.
	 */
	public SrgMappings() {
		super(NAME, false, false);
	}

	@Nonnull
	@Override
	public IntermediateMappings parse(@Nonnull String mappingText) {
		List<Pair<String, String>> packages = new ArrayList<>();
		IntermediateMappings mappings = new SrgIntermediateMappings(packages);
		String[] lines = StringUtil.splitNewline(mappingText);
		int line = 0;
		for (String lineStr : lines) {
			line++;
			String[] args = lineStr.trim().split(" ");
			String type = args[0];
			try {
				switch (type) {
					case "PK:" -> {
						String obfPackage = args[1];
						String renamedPackage = args[2];
						packages.add(new Pair<>(obfPackage, renamedPackage));
					}
					case "CL:" -> {
						String obfClass = args[1];
						String renamedClass = args[2];
						mappings.addClass(obfClass, renamedClass);
					}
					case "FD:" -> {
						// Common format:
						// 0  1
						// FD obf-owner/obf-name
						String obfKey = args[1];
						int splitPos = obfKey.lastIndexOf('/');
						String obfOwner = obfKey.substring(0, splitPos);
						String obfName = obfKey.substring(splitPos + 1);

						// Handle SRG variants
						if (args.length == 5) {
							// XSRG format:
							// 0  1                  2        3                      4
							// FD obf-owner/obf-name obf-desc clean-owner/clean-name clean-desc
							String obfDesc = args[2];
							String renamedKey = args[3];
							splitPos = renamedKey.lastIndexOf('/');
							String renamedName = renamedKey.substring(splitPos + 1);
							mappings.addField(obfOwner, obfDesc, obfName, renamedName);
						} else {
							// SRG format:
							// FD obf-owner/obf-name clean-owner/clean-name
							String renamedKey = args[2];
							splitPos = renamedKey.lastIndexOf('/');
							String renamedName = renamedKey.substring(splitPos + 1);
							mappings.addField(obfOwner, null, obfName, renamedName);
						}
					}
					case "MD:" -> {
						// Common format:
						// 0  1                  3
						// MD obf-owner/obf-name obf-desc
						String obfKey = args[1];
						int splitPos = obfKey.lastIndexOf('/');
						String obfOwner = obfKey.substring(0, splitPos);
						String obfName = obfKey.substring(splitPos + 1);
						String obfDesc = args[2];

						// Handle SRG variants
						if (args.length == 5) {
							// XSRG format:
							// 0  1                  2        3                      4
							// MD obf-owner/obf-name obf-desc clean-owner/clean-name clean-desc
							String renamedKey = args[3];
							splitPos = renamedKey.lastIndexOf('/');
							String renamedName = renamedKey.substring(splitPos + 1);
							mappings.addMethod(obfOwner, obfDesc, obfName, renamedName);
						} else {
							// SRG format:
							// 0  1                  2        3
							// MD obf-owner/obf-name obf-desc clean-owner/clean-name
							String renamedKey = args[3];
							splitPos = renamedKey.lastIndexOf('/');
							String renamedName = renamedKey.substring(splitPos + 1);
							mappings.addMethod(obfOwner, obfDesc, obfName, renamedName);
						}
					}
					default -> logger.trace("Unknown SRG mappings line type: \"{}\" @line {}", type, line);
				}
			} catch (IndexOutOfBoundsException ex) {
				throw new IllegalArgumentException("Failed parsing line " + line, ex);
			}
		}
		return mappings;
	}

	@Override
	public String exportText(@Nonnull Mappings mappings) {
		StringBuilder sb = new StringBuilder();
		Remapper remapper = new BasicMappingsRemapper(mappings);
		IntermediateMappings intermediate = mappings.exportIntermediate();
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

	/**
	 * Extension of intermediate mappings to support {@code PK} entries in the mapping file.
	 */
	private static class SrgIntermediateMappings extends IntermediateMappings {
		private final List<Pair<String, String>> packageMappings;

		public SrgIntermediateMappings(List<Pair<String, String>> packageMappings) {
			super();
			this.packageMappings = packageMappings;
		}

		@Override
		public boolean doesSupportFieldTypeDifferentiation() {
			// SRG fields do not include type info.
			return false;
		}

		@Override
		public boolean doesSupportVariableTypeDifferentiation() {
			// See above.
			return false;
		}

		@Nullable
		@Override
		public ClassMapping getClassMapping(String name) {
			ClassMapping classMapping = super.getClassMapping(name);
			if (classMapping == null && !packageMappings.isEmpty()) {
				for (Pair<String, String> packageMapping : packageMappings) {
					String oldPackage = packageMapping.getLeft();
					if (name.startsWith(oldPackage)) {
						String newPackage = packageMapping.getRight();
						return new ClassMapping(name, newPackage + name.substring(oldPackage.length()));
					}
				}
			}
			return classMapping;
		}
	}
}
