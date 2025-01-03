package software.coley.recaf.services.source;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.Mappings;
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
		if (before != null && after != null && !before.equals(after)) {
			Range namedRange = extractRelevantRange(named, source);
			if (namedRange.end() > source.length() || namedRange.isUnknown())
				return source;

			String namedSource = getSource(namedRange, source);
			String prefix = source.substring(0, namedRange.begin());
			String suffix = source.substring(namedRange.end());
			String replaced = namedSource.replace(before, after);

			// TODO: We should ensure only one replacement ever happens.
			//  - We could only replace if the content replaced is surrounded by boundaries.
			//  - Or just validate our range only has one instance of the 'before' text in it

			if (!replaced.equals(namedSource))
				return prefix + replaced + suffix;
		}
		return source;
	}

	@Nonnull
	private Range extractRelevantRange(@Nonnull Model model, @Nonnull String source) {
		if (model instanceof ClassModel classModel) {
			// The class model is often the FULL range of the file, and we only want the named section.
			String name = classModel.getName();
			int begin = classModel.getRange().begin();
			int end = source.indexOf(name, begin) + name.length();
			if (end < begin)
				return Range.UNKNOWN;
			return new Range(begin, end);
		} else if (model instanceof VariableModel variableModel) {
			// The variable range should include only the variable name.
			// The name doesn't have an associated model, but is after the declared type.
			String name = variableModel.getName();
			int begin = variableModel.getType().getRange().end();
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
	private static String getSource(@Nonnull Range range, @Nonnull String source) {
		int end = Math.min(source.length(), range.end());
		return source.substring(range.begin(), end);
	}

	private record NamedResolutions(@Nonnull NamedModel named, @Nonnull Resolution resolution) {}
}
