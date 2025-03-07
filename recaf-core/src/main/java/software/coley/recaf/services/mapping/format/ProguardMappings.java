package software.coley.recaf.services.mapping.format;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import net.fabricmc.mappingio.format.proguard.ProGuardFileWriter;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.Mappings;
import software.coley.recaf.util.StringUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Proguard mappings file implementation.
 *
 * @author xDark
 */
@Dependent
public class ProguardMappings extends AbstractMappingFileFormat {
	public static final String NAME = "Proguard";
	private static final String SPLITTER = " -> ";

	/**
	 * New proguard instance.
	 */
	public ProguardMappings() {
		super(NAME, true, false);
	}

	@Nonnull
	@Override
	public IntermediateMappings parse(@Nonnull String mappingsText) {
		IntermediateMappings mappings = new IntermediateMappings();
		List<String> lines = Arrays.asList(StringUtil.splitNewline(mappingsText));
		Map<String, ProguardClassInfo> classMap = new HashMap<>(16384);
		StringBuilder firstCache = new StringBuilder();
		StringBuilder secondCache = new StringBuilder();
		{
			// Collect class mappings
			ProguardClassInfo classInfo = null;
			int definitionStart = -1;
			for (int i = 0, j = lines.size(); i < j; i++) {
				String line = lines.get(i);
				if (line.isEmpty() || line.trim().charAt(0) == '#') {
					continue;
				}
				int index = line.indexOf(SPLITTER);
				String left = line.substring(0, index);
				String right = line.substring(index + SPLITTER.length());
				// Class mapping lines end with ':'
				if (right.charAt(right.length() - 1) == ':') {
					String originalClassName = left.replace('.', '/');
					String obfuscatedName = right.substring(0, right.length() - 1).replace('.', '/');
					mappings.addClass(obfuscatedName, originalClassName);
					if (classInfo != null) {
						// Record the lines that need to be processed for the prior classInfo entry
						//  - These lines should include field/method mappings
						classInfo.toProcess = lines.subList(definitionStart + 1, i);
					}
					classInfo = new ProguardClassInfo(obfuscatedName);
					classMap.put(originalClassName, classInfo);
					definitionStart = i;
				}
			}
			// Handle case for recording lines for the last class in the mappings file
			if (classInfo != null)
				classInfo.toProcess = lines.subList(definitionStart + 1, lines.size());
		}
		// Second pass for recording fields and methods
		for (ProguardClassInfo info : classMap.values()) {
			List<String> toProcess = info.toProcess;
			for (String line : toProcess) {
				if (line.isEmpty() || line.trim().charAt(0) == '#') {
					continue;
				}
				int index = line.indexOf(SPLITTER);
				String left = line.substring(0, index);
				String right = line.substring(index + SPLITTER.length());
				if (left.charAt(left.length() - 1) == ')') {
					int idx = left.indexOf(':');
					if (idx != -1) {
						idx = left.indexOf(':', idx + 1);
					}
					String methodInfo = idx == -1 ? left : left.substring(idx + 1);
					int offset = 0;
					while (methodInfo.charAt(offset) == ' ') {
						offset++;
					}
					String returnType = denormalizeType(methodInfo.substring(offset, offset = methodInfo.indexOf(' ', offset)), firstCache, classMap);
					firstCache.setLength(0);
					firstCache.append('(');
					String methodName = methodInfo.substring(offset + 1, offset = methodInfo.indexOf('('));
					int endOffset = methodInfo.indexOf(')', offset);
					parseDescriptor:
					{
						int typeStartOffset = methodInfo.indexOf(',', offset);
						if (typeStartOffset == -1) {
							if (endOffset == offset + 1) {
								break parseDescriptor;
							}
						}
						typeStartOffset = offset + 1;
						boolean anyLeft = true;
						do {
							int typeEndOfsset = methodInfo.indexOf(',', typeStartOffset);
							if (typeEndOfsset == -1) {
								anyLeft = false;
								typeEndOfsset = endOffset;
							}
							String type = denormalizeType(methodInfo.substring(typeStartOffset, typeEndOfsset), secondCache, classMap);
							firstCache.append(type);
							typeStartOffset = anyLeft ? methodInfo.indexOf(',', typeEndOfsset) + 1 : -1;
						} while (anyLeft);
					}
					firstCache.append(')').append(returnType);
					mappings.addMethod(info.mappedName, firstCache.toString(), right, methodName);
				} else {
					String fieldInfo = left;
					int offset = 0;
					while (fieldInfo.charAt(offset) == ' ') {
						offset++;
					}
					String fieldType = denormalizeType(fieldInfo.substring(offset, offset = fieldInfo.indexOf(' ', offset)), firstCache, classMap);
					String fieldName = fieldInfo.substring(offset + 1);
					mappings.addField(info.mappedName, fieldType, right, fieldName);
				}
			}
		}
		return mappings;
	}

	private static String denormalizeType(String type, StringBuilder stringCache, Map<String, ProguardClassInfo> map) {
		int dimensions = 0;
		int offset = 1;
		int idx;
		while (type.charAt((idx = type.length() - offset)) == ']') {
			dimensions++;
			offset += 2;
		}
		stringCache.setLength(0);
		type = type.substring(0, idx + 1);
		switch (type) {
			case "void" -> type = "V";
			case "long" -> type = "J";
			case "double" -> type = "D";
			case "int" -> type = "I";
			case "float" -> type = "F";
			case "char" -> type = "C";
			case "short" -> type = "S";
			case "byte" -> type = "B";
			case "boolean" -> type = "Z";
			default -> {
				type = type.replace('.', '/');
				ProguardClassInfo classInfo = map.get(type);
				if (classInfo != null) {
					type = classInfo.mappedName;
				}
				stringCache.append('L').append(type).append(';');
			}
		}
		if (dimensions != 0 || stringCache.length() != 0) {
			if (stringCache.length() == 0) {
				stringCache.append(type);
			}
			while (dimensions-- != 0) {
				stringCache.insert(0, '[');
			}
			type = stringCache.toString();
		}
		return type;
	}

	@Nullable
	@Override
	public String exportText(@Nonnull Mappings mappings) throws InvalidMappingException {
		return MappingFileFormat.export(mappings, ProGuardFileWriter::new);
	}

	private static final class ProguardClassInfo {
		private List<String> toProcess = List.of();
		private final String mappedName;

		ProguardClassInfo(String mappedName) {
			this.mappedName = mappedName;
		}
	}
}
