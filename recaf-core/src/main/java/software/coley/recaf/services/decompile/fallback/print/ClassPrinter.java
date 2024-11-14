package software.coley.recaf.services.decompile.fallback.print;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.Type;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.annotation.AnnotationElement;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.text.TextFormatConfig;
import software.coley.recaf.util.AccessFlag;
import software.coley.recaf.util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Basic class printer.
 *
 * @author Matt Coley
 */
public class ClassPrinter {
	private final TextFormatConfig format;
	private final JvmClassInfo classInfo;

	/**
	 * @param format
	 * 		Format config.
	 * @param classInfo
	 * 		Class to print.
	 */
	public ClassPrinter(@Nonnull TextFormatConfig format, @Nonnull JvmClassInfo classInfo) {
		this.format = format;
		this.classInfo = classInfo;
	}

	/**
	 * @return Formatted class output.
	 */
	@Nonnull
	public String print() {
		Printer out = new Printer();
		appendPackage(out);
		appendImports(out);
		appendDeclaration(out);
		appendMembers(out);
		return out.toString();
	}

	/**
	 * Appends the package name to the output.
	 *
	 * @param out
	 * 		Printer to write to.
	 */
	private void appendPackage(@Nonnull Printer out) {
		String className = classInfo.getName();
		if (className.contains("/")) {
			String packageName = format.filterEscape(className.substring(0, className.lastIndexOf('/')));
			out.appendLine("package " + packageName.replace('/', '.') + ";");
			out.newLine();
		}
	}

	/**
	 * Appends each imported class to the output.
	 *
	 * @param out
	 * 		Printer to write to.
	 */
	private void appendImports(@Nonnull Printer out) {
		String lastRootPackage = null;
		NavigableSet<String> referencedClasses = classInfo.getReferencedClasses();
		boolean hasImports = false;
		for (String referencedClass : referencedClasses) {
			// Skip classes in the default package.
			if (!referencedClass.contains("/")) continue;

			// Skip core classes that are implicitly imported.
			if (referencedClass.startsWith("java/lang/")) continue;

			// Skip self.
			if (referencedClass.equals(classInfo.getName())) continue;

			// Break root package imports up for clarity. For example:
			//  - com.*
			//  - org.*
			// Between these two import groups will be a blank line.
			String rootPackage = referencedClass.substring(0, referencedClass.indexOf('/'));
			if (lastRootPackage == null) lastRootPackage = rootPackage;
			if (!rootPackage.equals(lastRootPackage)) {
				out.newLine();
				lastRootPackage = rootPackage;
			}

			// Add import
			out.appendLine("import " + format.filterEscape(referencedClass.replace('/', '.')) + ";");
			hasImports = true;

			// TODO: Import names aren't always correct since '$' should also be escaped when it represents the separation of
			//     an outer and inner class. Since we have workspace and runtime access we 'should' check this
			//     and attempt to make more accurate output
		}
		if (hasImports) out.newLine();
	}

	/**
	 * Appends the class declaration to the output.
	 *
	 * @param out
	 * 		Printer to write to.
	 */
	private void appendDeclaration(@Nonnull Printer out) {
		appendDeclarationAnnotations(out);
		if (classInfo.hasEnumModifier()) {
			appendEnumDeclaration(out);
		} else if (classInfo.hasAnnotationModifier()) {
			appendAnnotationDeclaration(out);
		} else if (classInfo.hasInterfaceModifier()) {
			appendInterfaceDeclaration(out);
		} else {
			appendStandardDeclaration(out);
		}
	}

	/**
	 * Appends class annotations to the output.
	 *
	 * @param out
	 * 		Printer to write to.
	 */
	private void appendDeclarationAnnotations(@Nonnull Printer out) {
		String annotations = PrintUtils.annotationsToString(format, classInfo);
		if (!annotations.isBlank()) out.appendMultiLine(annotations);
	}

	/**
	 * Appends the enum formatted declaration to the output.
	 *
	 * @param out
	 * 		Printer to write to.
	 */
	private void appendEnumDeclaration(@Nonnull Printer out) {
		int acc = classInfo.getAccess();

		// Get flag-set and remove 'enum' and 'final'.
		// We will add 'enum' ourselves, and 'final' is redundant.
		Set<AccessFlag> flagSet = AccessFlag.getApplicableFlags(AccessFlag.Type.CLASS, acc);
		flagSet.remove(AccessFlag.ACC_ENUM);
		flagSet.remove(AccessFlag.ACC_FINAL);
		String decFlagsString = AccessFlag.sortAndToString(AccessFlag.Type.CLASS, flagSet);
		StringBuilder sb = new StringBuilder();
		if (decFlagsString.isBlank()) {
			sb.append("enum ");
		} else {
			sb.append(decFlagsString).append(" enum ");
		}
		sb.append(format.filter(classInfo.getName()));
		String superName = classInfo.getSuperName();

		// Should normally extend enum. Technically bytecode allows for other types if those at runtime then
		// inherit from Enum.
		if (superName != null && !superName.equals("java/lang/Enum")) {
			sb.append(" extends ").append(format.filter(superName));
		}
		if (!classInfo.getInterfaces().isEmpty()) {
			sb.append(" implements ");
			String interfaces = classInfo.getInterfaces().stream()
					.map(format::filter)
					.collect(Collectors.joining(", "));
			sb.append(interfaces);
		}
		out.appendLine(sb.toString());
	}

	/**
	 * Appends the annotation formatted declaration to the output.
	 *
	 * @param out
	 * 		Printer to write to.
	 */
	private void appendAnnotationDeclaration(@Nonnull Printer out) {
		int acc = classInfo.getAccess();

		// Get flag-set and remove 'interface' and 'abstract'.
		// We will add 'interface' ourselves, and 'abstract' is redundant.
		Set<AccessFlag> flagSet = AccessFlag.getApplicableFlags(AccessFlag.Type.CLASS, acc);
		flagSet.remove(AccessFlag.ACC_ANNOTATION);
		flagSet.remove(AccessFlag.ACC_INTERFACE);
		flagSet.remove(AccessFlag.ACC_ABSTRACT);
		String decFlagsString = AccessFlag.sortAndToString(AccessFlag.Type.CLASS, flagSet);
		StringBuilder sb = new StringBuilder();
		if (decFlagsString.isBlank()) {
			sb.append("@interface ");
		} else {
			sb.append(decFlagsString).append(" @interface ");
		}
		sb.append(format.filter(classInfo.getName()));
		out.appendLine(sb.toString());
	}

	/**
	 * Appends the interface formatted declaration to the output.
	 *
	 * @param out
	 * 		Printer to write to.
	 */
	private void appendInterfaceDeclaration(@Nonnull Printer out) {
		int acc = classInfo.getAccess();

		// Get flag-set and remove 'interface' and 'abstract'.
		// We will add 'interface' ourselves, and 'abstract' is redundant.
		Set<AccessFlag> flagSet = AccessFlag.getApplicableFlags(AccessFlag.Type.CLASS, acc);
		flagSet.remove(AccessFlag.ACC_INTERFACE);
		flagSet.remove(AccessFlag.ACC_ABSTRACT);
		String decFlagsString = AccessFlag.sortAndToString(AccessFlag.Type.CLASS, flagSet);
		StringBuilder sb = new StringBuilder();
		if (decFlagsString.isBlank()) {
			sb.append("interface ");
		} else {
			sb.append(decFlagsString)
					.append(" interface ");
		}
		sb.append(format.filter(classInfo.getName()));
		if (!classInfo.getInterfaces().isEmpty()) {
			// Interfaces use 'extends' rather than 'implements'.
			sb.append(" extends ");
			String interfaces = classInfo.getInterfaces().stream()
					.map(format::filter)
					.collect(Collectors.joining(", "));
			sb.append(interfaces);
		}
		out.appendLine(sb.toString());
	}

	/**
	 * Appends the class formatted declaration to the output.
	 *
	 * @param out
	 * 		Printer to write to.
	 */
	private void appendStandardDeclaration(@Nonnull Printer out) {
		int acc = classInfo.getAccess();
		String decFlagsString = AccessFlag.sortAndToString(AccessFlag.Type.CLASS, acc);
		StringBuilder sb = new StringBuilder();
		if (decFlagsString.isBlank()) {
			sb.append("class ");
		} else {
			sb.append(decFlagsString).append(" class ");
		}
		sb.append(format.filter(classInfo.getName()));
		String superName = classInfo.getSuperName();
		if (superName != null && !superName.equals("java/lang/Object")) {
			sb.append(" extends ").append(format.filter(superName));
		}
		if (!classInfo.getInterfaces().isEmpty()) {
			sb.append(" implements ");
			String interfaces = classInfo.getInterfaces().stream()
					.map(format::filter)
					.collect(Collectors.joining(", "));
			sb.append(interfaces);
		}
		out.appendLine(sb.toString());
	}

	/**
	 * Appends the class body (members).
	 *
	 * @param out
	 * 		Printer to write to.
	 */
	private void appendMembers(@Nonnull Printer out) {
		out.appendLine("{");
		if (!classInfo.getFields().isEmpty()) {
			Printer fieldPrinter = new Printer();
			fieldPrinter.setIndent("    ");
			if (classInfo.hasEnumModifier()) {
				appendEnumFieldMembers(fieldPrinter);
			} else {
				appendFieldMembers(fieldPrinter);
			}
			out.appendMultiLine(fieldPrinter.toString());
			out.appendLine("");
		}
		if (!classInfo.getMethods().isEmpty()) {
			Printer methodPrinter = new Printer();
			methodPrinter.setIndent("    ");

			// Some method types we'll want to handle a bit differently.
			// Split them up:
			//  - Regular methods
			//  - The static initializer
			//  - Constructors
			List<MethodMember> methods = new ArrayList<>(classInfo.getMethods());
			MethodMember staticInitializer = classInfo.getDeclaredMethod("<clinit>", "()V");
			List<MethodMember> constructors = classInfo.methodStream()
					.filter(m -> m.getName().equals("<init>"))
					.toList();
			methods.remove(staticInitializer);
			methods.removeAll(constructors);

			// We'll place the static initializer first regardless of where its defined order-wise.
			if (staticInitializer != null) {
				appendStaticInitializer(methodPrinter, staticInitializer);
				methodPrinter.newLine();
			}

			// Then the constructors.
			for (MethodMember constructor : constructors) {
				appendConstructor(methodPrinter, constructor);
				methodPrinter.newLine();
			}

			// Then the rest of the methods, in whatever order they're defined in.
			for (MethodMember method : methods) {
				appendMethod(methodPrinter, method);
				methodPrinter.newLine();
			}

			// Append them all to the output.
			out.appendMultiLine(methodPrinter.toString());
		}
		out.appendLine("}");
	}

	/**
	 * Appends all fields in the class.
	 *
	 * @param out
	 * 		Printer to write to.
	 *
	 * @see #appendEnumFieldMembers(Printer) To be used when the current class is an enum.
	 */
	private void appendFieldMembers(@Nonnull Printer out) {
		for (FieldMember field : classInfo.getFields()) {
			appendField(out, field);
		}
	}

	/**
	 * Appends all enum constants, then other fields in the class.
	 *
	 * @param out
	 * 		Printer to write to.
	 *
	 * @see #appendEnumFieldMembers(Printer) To be used when the current class is not an enum.
	 */
	private void appendEnumFieldMembers(@Nonnull Printer out) {
		// Filter out enum constants
		List<FieldMember> enumConstFields = new ArrayList<>();
		List<FieldMember> otherFields = new ArrayList<>();
		for (FieldMember field : classInfo.getFields()) {
			if (isEnumConst(field)) {
				enumConstFields.add(field);
			} else {
				otherFields.add(field);
			}
		}

		// Print enum constants first.
		for (int i = 0; i < enumConstFields.size(); i++) {
			String suffix = i == enumConstFields.size() - 1 ? ";\n" : ", ";
			FieldMember enumConst = enumConstFields.get(i);
			StringBuilder sb = new StringBuilder();
			String annotations = PrintUtils.annotationsToString(format, enumConst);
			if (!annotations.isBlank())
				sb.append(annotations).append('\n');
			sb.append(enumConst.getName()).append(suffix);
			out.appendMultiLine(sb.toString());
		}
		out.newLine();

		// And then the rest of the fields
		for (FieldMember field : otherFields) {
			appendField(out, field);
		}
	}

	/**
	 * Appends the given field.
	 *
	 * @param out
	 * 		Printer to write to.
	 * @param field
	 * 		Field to write to the given printer.
	 */
	private void appendField(@Nonnull Printer out, @Nonnull FieldMember field) {
		StringBuilder declaration = new StringBuilder();

		// Append annotations to builder.
		String annotations = PrintUtils.annotationsToString(format, field);
		if (!annotations.isBlank())
			declaration.append(annotations).append('\n');

		// Append flags to builder.
		Collection<AccessFlag> flags = AccessFlag.getApplicableFlags(AccessFlag.Type.FIELD, field.getAccess());
		flags.remove(AccessFlag.ACC_ENUM); // We don't want to print 'enum' as a flag
		flags = AccessFlag.sort(AccessFlag.Type.FIELD, flags);
		if (!flags.isEmpty())
			declaration.append(AccessFlag.toString(flags)).append(' ');

		// Append type + name to builder.
		Type type = Type.getType(field.getDescriptor());
		String typeName = format.filter(type.getClassName());
		if (typeName.contains("."))
			typeName = typeName.substring(typeName.lastIndexOf(".") + 1);
		declaration.append(typeName).append(' ').append(format.filter(field.getName()));

		// Append value to builder.
		Object value = field.getDefaultValue();
		if (value != null) {
			switch (value) {
				case String s -> value = "\"" + format.filter(s) + "\"";
				case Float v -> value = value + "F";
				case Long l -> value = value + "L";
				default -> {
					// No change
				}
			}
			declaration.append(" = ").append(value);
		}

		// Cap it off.
		declaration.append(';');
		out.appendMultiLine(declaration.toString());
	}

	/**
	 * @param field
	 * 		Field to check.
	 *
	 * @return {@code true} when it is an enum constant of the {@link #classInfo current class}.
	 */
	private boolean isEnumConst(@Nonnull FieldMember field) {
		String descriptor = field.getDescriptor();
		if (descriptor.length() < 3) return false;

		String type = descriptor.substring(1, descriptor.length() - 1);

		// Must be same type as declaring class.
		if (!type.equals(classInfo.getName())) return false;

		// Must have enum const flags
		return AccessFlag.hasAll(field.getAccess(), AccessFlag.ACC_STATIC, AccessFlag.ACC_FINAL);
	}

	/**
	 * Appends the given static initializer method.
	 *
	 * @param out
	 * 		Printer to write to.
	 * @param method
	 * 		Static initializer method.
	 */
	private void appendStaticInitializer(@Nonnull Printer out, @Nonnull MethodMember method) {
		MethodPrinter clinitPrinter = new MethodPrinter(format, classInfo, method) {
			@Override
			protected void buildDeclarationFlags(@Nonnull StringBuilder sb) {
				// Force only printing the modifier 'static' even if other flags are present
				sb.append("static");
			}

			@Override
			protected void buildDeclarationReturnType(@Nonnull StringBuilder sb) {
				// no-op
			}

			@Override
			protected void buildDeclarationName(@Nonnull StringBuilder sb) {
				// no-op
			}

			@Override
			protected void buildDeclarationArgs(@Nonnull StringBuilder sb) {
				// no-op
			}

			@Override
			protected void buildDeclarationThrows(@Nonnull StringBuilder sb) {
				// no-op
			}
		};
		out.appendMultiLine(clinitPrinter.print());
	}

	/**
	 * Appends the given constructor method.
	 *
	 * @param out
	 * 		Printer to write to.
	 * @param method
	 * 		Constructor method.
	 */
	private void appendConstructor(@Nonnull Printer out, @Nonnull MethodMember method) {
		MethodPrinter constructorPrinter = new MethodPrinter(format, classInfo, method) {
			@Override
			protected void buildDeclarationReturnType(@Nonnull StringBuilder sb) {
				// no-op
			}

			@Override
			protected void buildDeclarationName(@Nonnull StringBuilder sb) {
				// The name is always the class name
				sb.append(format.filterEscape(StringUtil.shortenPath(classInfo.getName())));
			}
		};
		out.appendMultiLine(constructorPrinter.print());
	}

	/**
	 * Appends the given method.
	 *
	 * @param out
	 * 		Printer to write to.
	 * @param method
	 * 		Regular method.
	 */
	private void appendMethod(@Nonnull Printer out, @Nonnull MethodMember method) {
		if (classInfo.hasAnnotationModifier()) {
			MethodPrinter constructorPrinter = new MethodPrinter(format, classInfo, method) {
				@Override
				protected void buildDeclarationFlags(@Nonnull StringBuilder sb) {
					// no-op since all methods are 'public abstract' per interface contract (with additional restrictions)
				}

				@Override
				protected void appendAbstractBody(@Nonnull StringBuilder sb) {
					AnnotationElement annotationDefault = method.getAnnotationDefault();
					if (annotationDefault != null) {
						sb.append(" default ").append(PrintUtils.elementToString(format, annotationDefault)).append(";");
					} else {
						sb.append(";");
					}
				}
			};
			out.appendMultiLine(constructorPrinter.print());
		} else if (classInfo.hasInterfaceModifier()) {
			MethodPrinter constructorPrinter = new MethodPrinter(format, classInfo, method) {
				@Override
				protected void buildDeclarationFlags(@Nonnull StringBuilder sb) {
					Collection<AccessFlag> flags = AccessFlag.getApplicableFlags(AccessFlag.Type.METHOD, method.getAccess());
					flags = AccessFlag.sort(AccessFlag.Type.METHOD, flags);
					flags.remove(AccessFlag.ACC_PUBLIC);
					flags.remove(AccessFlag.ACC_ABSTRACT);
					boolean isAbstract = AccessFlag.isAbstract(method.getAccess());
					if (!flags.isEmpty()) {
						String flagsStr = AccessFlag.toString(flags);
						if (!isAbstract)
							sb.append("default ");
						sb.append(flagsStr).append(' ');
					} else if (!isAbstract)
						sb.append("default ");
				}
			};
			out.appendMultiLine(constructorPrinter.print());
		} else {
			out.appendMultiLine(new MethodPrinter(format, classInfo, method).print());
		}
	}
}
