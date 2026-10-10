package software.coley.recaf.services.source;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.Mappings;
import software.coley.recaf.util.AccessFlag;
import software.coley.recaf.util.StringUtil;
import software.coley.sourcesolver.model.ClassModel;
import software.coley.sourcesolver.model.CompilationUnitModel;
import software.coley.sourcesolver.model.ImportModel;
import software.coley.sourcesolver.model.MethodModel;
import software.coley.sourcesolver.model.Model;
import software.coley.sourcesolver.model.NameHoldingModel;
import software.coley.sourcesolver.model.NamedModel;
import software.coley.sourcesolver.model.VariableModel;
import software.coley.sourcesolver.resolve.Resolver;
import software.coley.sourcesolver.resolve.entry.ClassEntry;
import software.coley.sourcesolver.resolve.entry.ClassMemberPair;
import software.coley.sourcesolver.resolve.entry.FieldEntry;
import software.coley.sourcesolver.resolve.entry.MemberEntry;
import software.coley.sourcesolver.resolve.entry.MethodEntry;
import software.coley.sourcesolver.resolve.result.ClassResolution;
import software.coley.sourcesolver.resolve.result.FieldResolution;
import software.coley.sourcesolver.resolve.result.MethodResolution;
import software.coley.sourcesolver.resolve.result.MultiClassResolution;
import software.coley.sourcesolver.resolve.result.MultiMemberResolution;
import software.coley.sourcesolver.resolve.result.PrimitiveResolution;
import software.coley.sourcesolver.resolve.result.Resolution;
import software.coley.sourcesolver.resolve.result.VariableResolution;
import software.coley.sourcesolver.util.Range;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Replaces identifiers in a {@link CompilationUnitModel} with new names based on provided {@link Mappings}.
 *
 * @author Matt Coley
 */
@SuppressWarnings("IfCanBeSwitch")
public class AstMapper {
	private final CompilationUnitModel unit;
	private final Resolver resolver;
	private final Mappings mappings;

	/**
	 * @param unit
	 * 		Unit to map.
	 * @param resolver
	 * 		Resolver to analyze the unit with.
	 * @param mappings
	 * 		Mappings to apply.
	 */
	public AstMapper(@Nonnull CompilationUnitModel unit, @Nonnull Resolver resolver, @Nonnull Mappings mappings) {
		this.unit = unit;
		this.resolver = resolver;
		this.mappings = mappings;
	}

	/**
	 * @return Modified source code based on the provided mappings.
	 */
	@Nonnull
	public String apply() {
		String source = unit.getInputSource();

		// Get all named resolutions and replace them with their mapped alternatives in reverse order.
		List<NamedResolutions> pairs = new ArrayList<>();
		unit.visit(model -> {
			if (!model.getRange().isUnknown() && model instanceof NamedModel named) {
				Resolution resolution = model.resolve(resolver);
				if (!resolution.isUnknown() && !(resolution instanceof PrimitiveResolution))
					pairs.add(new NamedResolutions(named, resolution));
			}
			return true;
		});
		for (int i = pairs.size() - 1; i >= 0; i--) {
			NamedResolutions pair = pairs.get(i);
			NamedModel named = pair.named();
			Resolution resolution = pair.resolution();
			if (resolution instanceof ClassResolution classResolution) {
				source = replacePatternIn(named, source, getSimpleName(classResolution), getSimpleName(getMappedClass(classResolution)));
			} else if (resolution instanceof FieldResolution fieldResolution) {
				source = replacePatternIn(named, source, fieldResolution.getFieldEntry().getName(), getSimpleName(getMappedField(fieldResolution)));
			} else if (resolution instanceof MethodResolution methodResolution) {
				if (methodResolution.getMethodEntry().getName().equals("<init>")) {
					// Constructors get replaced as the owner name
					ClassResolution ownerResolution = methodResolution.getOwnerResolution();
					source = replacePatternIn(named, source, getSimpleName(ownerResolution), getSimpleName(getMappedClass(ownerResolution)));
				} else {
					source = replacePatternIn(named, source, methodResolution.getMethodEntry().getName(), getSimpleName(getMappedMethod(methodResolution)));
				}
			} else if (resolution instanceof VariableResolution variableResolution) {
				source = replacePatternIn(named, source, variableResolution.getName(), getMappedVariable(variableResolution, named));
			}
		}

		// Replace imports of mapped classes & members.
		IntermediateMappings intermediateMappings = mappings.exportIntermediate();
		for (int i = unit.getImports().size() - 1; i >= 0; i--) {
			ImportModel importModel = unit.getImports().get(i);
			Resolution resolution = importModel.resolve(resolver);
			if (resolution instanceof ClassResolution classResolution) {
				// Single class to map.
				String mappedName = getMappedClass(classResolution);
				if (mappedName != null) {
					String baseName = classResolution.getClassEntry().getName().replace('/', '.');
					source = replacePatternIn(importModel, source, baseName, mappedName.replace('/', '.'));
				}
			} else if (resolution instanceof MultiClassResolution multiClassResolution) {
				// Multiple classes to consider with a 'package.*' import.
				List<String> classesInPackage = multiClassResolution.getClassEntries().stream()
						.map(ClassEntry::getName)
						.toList();
				Set<String> unmappedClasses = new HashSet<>();
				Map<String, String> mappedClasses = new HashMap<>();
				for (String className : classesInPackage) {
					String mappedClassName = mappings.getMappedClassName(className);
					if (mappedClassName != null)
						mappedClasses.put(className, mappedClassName);
					else
						unmappedClasses.add(className);
				}

				// No classes were mapped, so we're good to go
				if (mappedClasses.isEmpty())
					continue;

				// Determine what new package names we need to import.
				int begin = importModel.getRange().begin();
				boolean removeExistingPackageImport = unmappedClasses.isEmpty();
				if (removeExistingPackageImport)
					source = source.replace(importModel.getSource(unit), "");
				for (String mappedClass : mappedClasses.values()) {
					if (mappedClass.indexOf('$') < 0) // Skip inner classes
						source = StringUtil.insert(source, begin, "import " + mappedClass.replace('/', '.') + ";\n");
				}
			} else if (resolution instanceof MultiMemberResolution multiMemberResolution) {
				ClassEntry ownerEntry = multiMemberResolution.getMemberEntries().getFirst().ownerEntry();

				// Rename the imported field/method names if they were mapped.
				if (importModel.getName().indexOf('*') < 0) {
					for (ClassMemberPair pair : multiMemberResolution.getMemberEntries()) {
						MemberEntry memberEntry = pair.memberEntry();
						String mappedMemberName;
						if (memberEntry instanceof FieldEntry fieldEntry) {
							mappedMemberName = getMappedField(ownerEntry, fieldEntry);
						} else if (memberEntry instanceof MethodEntry methodEntry) {
							mappedMemberName = getMappedMethod(ownerEntry, methodEntry);
						} else {
							mappedMemberName = null;
						}
						if (mappedMemberName != null) {
							source = replacePatternIn(importModel, source, "." + memberEntry.getName() + ";", "." + mappedMemberName + ";");
							break;
						}
					}
				}

				// If the owning class was renamed, then rename that.
				String mappedClass = getMappedClass(ownerEntry);
				if (mappedClass != null) {
					source = replacePatternIn(importModel, source,
							ownerEntry.getName().replace('/', '.'),
							mappedClass.replace('/', '.'));
				}
			}
		}

		// Replace package if the class got moved to a different package.
		if (unit.getDeclaredClasses().getFirst().resolve(resolver) instanceof ClassResolution resolution) {
			String mappedClass = getMappedClass(resolution);
			if (mappedClass != null) {
				int slashIndex = mappedClass.lastIndexOf('/');
				if (slashIndex > 0) {
					String mappedPackageName = mappedClass.substring(0, slashIndex);
					String packageName = unit.getPackage().getName().replace('.', '/');
					if (!packageName.equals(mappedPackageName)) {
						source = source.replace("package " + unit.getPackage().getName() + ";",
								"package " + mappedPackageName.replace('/', '.') + ";");
					}
				}
			}
		}

		return source;
	}

	@Nonnull
	private String replacePatternIn(@Nonnull Model named, @Nonnull String source,
	                                @Nullable String before, @Nullable String after) {
		if (before == null || after == null || before.isEmpty() || before.equals(after))
			return source;

		// Determine the range of the named model to limit the search for the pattern.
		Range namedRange = extractRelevantRange(named, source);
		int begin = namedRange.begin();
		int end = namedRange.end();
		if (namedRange.isUnknown() || begin < 0 || end > source.length() || end < begin)
			return source;

		// If the pattern is a valid identifier, then we need to ensure that the match is bounded by non-identifier characters.
		boolean identifier = isJavaIdentifier(before);
		int match = -1;
		int lastStart = end - before.length();
		for (int search = begin; search <= lastStart; ) {
			int candidate = source.indexOf(before, search);
			if (candidate < 0 || candidate > lastStart)
				break;

			if (!identifier || hasIdentifierBoundaries(source, candidate, candidate + before.length())) {
				if (match >= 0)
					return source;
				match = candidate;
			}
			search = candidate + 1;
		}

		// No match.
		if (match < 0)
			return source;

		// Replace the match with the new name.
		return source.substring(0, match) + after + source.substring(match + before.length());
	}

	@Nonnull
	private Range extractRelevantRange(@Nonnull Model model, @Nonnull String source) {
		if (model instanceof ClassModel classModel) {
			// Class model ranges may include annotations, so prefer the exact range of the name model.
			if (classModel.getNameModel() != null && !classModel.getNameModel().getRange().isUnknown())
				return classModel.getNameModel().getRange();

			// Otherwise, locate the identifier after the class declaration keyword. The range can include annotations
			// that mention the class name, so selecting the first textual match is not sufficient.
			return findClassNameRange(classModel, source);
		} else if (model instanceof VariableModel variableModel) {
			// The variable range should include only the variable name.
			// The name doesn't have an associated model, but is after the declared type.
			String name = variableModel.getName();
			int begin = variableModel.getType().getRange().end();

			// Enum constants don't have an AST model for their type, so the range is "unknown".
			// If we're confident this is an enum constant, then we'll make the beginning range the start of the field name instead.
			if (begin == -1) {
				ClassModel declaringClass = variableModel.getParentOfType(ClassModel.class);
				if (declaringClass != null) {
					if (declaringClass.resolve(resolver) instanceof ClassResolution declaringResolution
							&& AccessFlag.isEnum(declaringResolution.getClassEntry().getAccess())) {
						Resolution type = variableModel.getType().resolve(resolver);
						if (type.matches(declaringResolution)) {
							begin = variableModel.getRange().begin();
						}
					}
				}
			}

			int end = source.indexOf(name, begin) + name.length();
			if (end < begin)
				return Range.UNKNOWN;
			return new Range(begin, end);
		} else if (model instanceof MethodModel methodModel) {
			// The method range should include only the method name.
			// The name doesn't have an associated model, but is between the return type and first '(' for parameters.
			String name = methodModel.getName();
			int begin = name.equals("<init>") ? methodModel.getRange().begin() : methodModel.getReturnType().getRange().end();
			int end = source.indexOf('(', begin);
			if (end < begin)
				return Range.UNKNOWN;
			return new Range(begin, end);
		} else if (model instanceof NameHoldingModel nameHoldingModel) {
			// If the holder has an associated model, yield that model's range.
			if (nameHoldingModel.getNameModel() != null && !nameHoldingModel.getNameModel().getRange().isUnknown())
				return nameHoldingModel.getNameModel().getRange();

			// Otherwise limit the size of the range based on the first appearance of the named model's name
			// starting from its reported range beginning point. Also limit by the source length.
			String name = nameHoldingModel.getName();
			Range range = nameHoldingModel.getRange();
			int nameBegin = source.indexOf(name, range.begin());
			int nameEnd = Math.min(nameBegin + name.length(), source.length());
			return new Range(nameBegin, nameEnd);
		}

		return model.getRange();
	}

	@Nullable
	private String getMappedClass(@Nonnull ClassResolution resolution) {
		return getMappedClass(resolution.getClassEntry());
	}

	@Nullable
	private String getMappedClass(@Nonnull ClassEntry classEntry) {
		String name = classEntry.getName();
		return mappings.getMappedClassName(name);
	}

	@Nullable
	private String getMappedField(@Nonnull FieldResolution resolution) {
		return getMappedField(resolution.getOwnerEntry(), resolution.getFieldEntry());
	}

	@Nullable
	private String getMappedField(@Nonnull ClassEntry ownerEntry, @Nonnull FieldEntry fieldEntry) {
		String owner = ownerEntry.getName();
		String name = fieldEntry.getName();
		String desc = fieldEntry.getDescriptor();
		return mappings.getMappedFieldName(owner, name, desc);
	}

	@Nullable
	private String getMappedMethod(@Nonnull MethodResolution resolution) {
		return getMappedMethod(resolution.getOwnerEntry(), resolution.getMethodEntry());
	}

	@Nullable
	private String getMappedMethod(@Nonnull ClassEntry ownerEntry, @Nonnull MethodEntry methodEntry) {
		String owner = ownerEntry.getName();
		String name = methodEntry.getName();
		String desc = methodEntry.getDescriptor();
		return mappings.getMappedMethodName(owner, name, desc);
	}

	@Nullable
	private String getMappedVariable(@Nonnull VariableResolution resolution, @Nonnull NamedModel named) {
		if (!(named instanceof Model model))
			return null;

		MethodModel method = model instanceof MethodModel methodModel ? methodModel : model.getParentOfType(MethodModel.class);
		if (method == null || !(method.resolve(resolver) instanceof MethodResolution methodResolution))
			return null;

		ClassEntry owner = methodResolution.getOwnerEntry();
		MethodEntry methodEntry = methodResolution.getMethodEntry();
		return mappings.getMappedVariableName(owner.getName(), methodEntry.getName(), methodEntry.getDescriptor(),
				resolution.getName(), resolution.getResolvedType().getDescriptor(), -1);
	}

	@Nonnull
	private static String getSimpleName(@Nonnull ClassResolution resolution) {
		return getSimpleName(resolution.getClassEntry());
	}

	@Nonnull
	private static String getSimpleName(@Nonnull ClassEntry entry) {
		return Objects.requireNonNull(getSimpleName(entry.getName()));
	}

	@Nullable
	private static String getSimpleName(@Nullable String name) {
		if (name == null)
			return null;
		return StringUtil.shortenPath(name);
	}

	@Nonnull
	private static Range findClassNameRange(@Nonnull ClassModel classModel, @Nonnull String source) {
		Range range = classModel.getRange();
		int begin = range.begin();
		int end = Math.min(range.end(), source.length());
		if (range.isUnknown() || begin < 0 || begin > end)
			return Range.UNKNOWN;

		String name = classModel.getName();
		boolean expectingName = false;
		for (int i = begin; i < end; ) {
			char current = source.charAt(i);
			if (Character.isWhitespace(current)) {
				i++;
				continue;
			}

			// Comments may occur between the declaration keyword and name, and should not contribute tokens.
			if (current == '/' && i + 1 < end) {
				char next = source.charAt(i + 1);
				if (next == '/') {
					i += 2;
					while (i < end && source.charAt(i) != '\n' && source.charAt(i) != '\r')
						i++;
					continue;
				} else if (next == '*') {
					i += 2;
					while (i + 1 < end && (source.charAt(i) != '*' || source.charAt(i + 1) != '/'))
						i++;
					i = Math.min(end, i + 2);
					continue;
				}
			}

			if (current == '"' || current == '\'') {
				i = skipQuotedLiteral(source, i, end, current);
				expectingName = false;
				continue;
			}

			int codePoint = source.codePointAt(i);
			if (Character.isJavaIdentifierStart(codePoint)) {
				int tokenBegin = i;
				i += Character.charCount(codePoint);
				while (i < end) {
					codePoint = source.codePointAt(i);
					if (!Character.isJavaIdentifierPart(codePoint))
						break;
					i += Character.charCount(codePoint);
				}

				int tokenLength = i - tokenBegin;
				if (expectingName && tokenLength == name.length() && source.regionMatches(tokenBegin, name, 0, tokenLength))
					return new Range(tokenBegin, i);
				expectingName = isClassDeclarationKeyword(source, tokenBegin, tokenLength);
				continue;
			}

			i += Character.charCount(codePoint);
			expectingName = false;
		}

		return Range.UNKNOWN;
	}

	private static boolean isJavaIdentifier(@Nonnull String value) {
		if (value.isEmpty())
			return false;

		int index = 0;
		int codePoint = value.codePointAt(index);
		if (!Character.isJavaIdentifierStart(codePoint))
			return false;

		index += Character.charCount(codePoint);
		while (index < value.length()) {
			codePoint = value.codePointAt(index);
			if (!Character.isJavaIdentifierPart(codePoint))
				return false;
			index += Character.charCount(codePoint);
		}
		return true;
	}

	private static boolean hasIdentifierBoundaries(@Nonnull String source, int begin, int end) {
		return (begin == 0 || !Character.isJavaIdentifierPart(source.codePointBefore(begin)))
				&& (end == source.length() || !Character.isJavaIdentifierPart(source.codePointAt(end)));
	}

	private static boolean isClassDeclarationKeyword(@Nonnull String source, int begin, int length) {
		return (length == 5 && source.regionMatches(begin, "class", 0, length))
				|| (length == 9 && source.regionMatches(begin, "interface", 0, length))
				|| (length == 4 && source.regionMatches(begin, "enum", 0, length))
				|| (length == 6 && source.regionMatches(begin, "record", 0, length));
	}

	private static int skipQuotedLiteral(@Nonnull String source, int begin, int end, char quote) {
		boolean textBlock = quote == '"' && begin + 2 < end
				&& source.charAt(begin + 1) == '"'
				&& source.charAt(begin + 2) == '"';
		int delimiterLength = textBlock ? 3 : 1;
		int index = begin + delimiterLength;
		while (index < end) {
			char current = source.charAt(index);
			if (current == '\\') {
				index = Math.min(end, index + 2);
				continue;
			}
			if (current == quote && (!textBlock || index + 2 < end
					&& source.charAt(index + 1) == '"'
					&& source.charAt(index + 2) == '"'))
				return index + delimiterLength;
			index++;
		}
		return end;
	}

	private record NamedResolutions(@Nonnull NamedModel named, @Nonnull Resolution resolution) {}
}
