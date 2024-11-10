package software.coley.recaf.services.assembler;

import dev.xdark.blw.type.Types;
import dev.xdark.blw.type.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import me.darknet.assembler.printer.JvmClassPrinter;
import me.darknet.assembler.printer.JvmMethodPrinter;
import me.darknet.assembler.printer.PrintContext;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import regexodus.Matcher;
import regexodus.Pattern;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.BasicLocalVariable;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.LocalVariable;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.compile.CompilerDiagnostic;
import software.coley.recaf.services.compile.CompilerResult;
import software.coley.recaf.services.compile.JavacArguments;
import software.coley.recaf.services.compile.JavacCompiler;
import software.coley.recaf.util.*;
import software.coley.recaf.workspace.model.Workspace;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Compiles Java source expressions into JASM.
 *
 * @author Matt Coley
 */
@Dependent
public class ExpressionCompiler {
	private static final Logger logger = Logging.get(ExpressionCompiler.class);
	private static final Pattern IMPORT_EXTRACT_PATTERN = RegexUtil.pattern("^\\s*(import \\w.+;)");
	private static final String EXPR_MARKER = "/* EXPR_START */";
	private final JavacCompiler javac;
	private final Workspace workspace;
	private final AssemblerPipelineGeneralConfig assemblerConfig;
	private int classAccess;
	private String className;
	private String superName;
	private List<String> implementing;
	private int versionTarget;
	private List<FieldMember> fields;
	private List<MethodMember> methods;
	private String methodName;
	private MethodType methodType;
	private int methodFlags;
	private List<LocalVariable> methodVariables;

	@Inject
	public ExpressionCompiler(@Nonnull Workspace workspace, @Nonnull JavacCompiler javac,
	                          @Nonnull AssemblerPipelineGeneralConfig assemblerConfig) {
		this.workspace = workspace;
		this.javac = javac;
		this.assemblerConfig = assemblerConfig;
		clearContext();
	}

	/**
	 * Resets the assembler to have no class or method context.
	 */
	public void clearContext() {
		className = "RecafExpression";
		classAccess = 0;
		superName = null;
		implementing = Collections.emptyList();
		versionTarget = JavaVersion.get();
		fields = Collections.emptyList();
		methods = Collections.emptyList();
		methodName = "generated";
		methodType = Types.methodType("()V");
		methodFlags = Opcodes.ACC_STATIC | Opcodes.ACC_BRIDGE; // Bridge used to denote default state.
		methodVariables = Collections.emptyList();
	}

	/**
	 * Updates the expression compiler to create the expression within the given class.
	 * This allows access to the class's fields, methods, and type hierarchy.
	 *
	 * @param classInfo
	 * 		Class to pull info from.
	 */
	public void setClassContext(@Nonnull JvmClassInfo classInfo) {
		String type = classInfo.getName();
		String superType = classInfo.getSuperName();
		className = isSafeInternalClassName(type) ? type : "obfuscated_class";
		classAccess = classInfo.getAccess();
		versionTarget = NumberUtil.intClamp(classInfo.getVersion() - JvmClassInfo.BASE_VERSION, JavacCompiler.getMinTargetVersion(), JavaVersion.get());
		superName = superType != null && isSafeInternalClassName(superType) ? superType : null;
		implementing = classInfo.getInterfaces().stream()
				.filter(ExpressionCompiler::isSafeInternalClassName)
				.toList();
		fields = classInfo.getFields();
		methods = classInfo.getMethods();

		// We use bridge to denote that the default flags are set.
		// When we assign a class, there may be non-static fields/methods the user will want to interact with.
		// Thus, we should clear our flags from the default so they can do that.
		if (AccessFlag.isBridge(methodFlags))
			methodFlags = 0;

		// TODO: Support for generics (For example, if we implement Supplier<String> and we have a method "String get()")
		//  - Also will want per-method signatures for things like 'List<String> strings' as a parameter
		//  - https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.9.1
	}

	/**
	 * Updates the expression compiler to create the expression within the given method.
	 * This allows access to the method's parameters.
	 *
	 * @param method
	 * 		Method to pull info from.
	 */
	public void setMethodContext(@Nonnull MethodMember method) {
		// Map edge cases for disallowed names.
		String name = method.getName();
		if (name.equals("<init>"))
			name = "instance_ctor";
		else if (name.equals("<clinit>"))
			name = "static_ctor";
		else if (!isSafeName(name))
			name = "obfuscated_method";

		methodName = name;
		methodType = Types.methodType(method.getDescriptor());
		methodFlags = method.getAccess();
		methodVariables = method.getLocalVariables();
	}

	/**
	 * Set the target version of Java.
	 * <br>
	 * Java 8 would pass 8.
	 *
	 * @param versionTarget
	 * 		Java version target.
	 * 		Range of supported values: [{@link JavacCompiler#getMinTargetVersion()} - {@link JavaVersion#get()}]
	 */
	public void setVersionTarget(int versionTarget) {
		this.versionTarget = versionTarget;
	}

	/**
	 * Compiles the given expression with the current context.
	 *
	 * @param expression
	 * 		Expression to compile.
	 *
	 * @return Expression compilation result.
	 *
	 * @see #setClassContext(JvmClassInfo) For allowing access to a class's fields/methods/inheritance.
	 * @see #setMethodContext(MethodMember) For allowing access to a method's parameters & other local variables.
	 */
	@Nonnull
	public ExpressionResult compile(@Nonnull String expression) {
		// Generate source of a class to house the expression within
		String code;
		try {
			code = generateClass(expression);
		} catch (ExpressionCompileException ex) {
			return new ExpressionResult(ex);
		}

		// Compile the generated class
		JavacArguments arguments = new JavacArguments(className, code, null, Math.max(versionTarget, JavacCompiler.getMinTargetVersion()), -1, true, false, false);
		CompilerResult result = javac.compile(arguments, workspace, null);
		if (!result.wasSuccess()) {
			Throwable exception = result.getException();
			if (exception != null)
				return new ExpressionResult(new ExpressionCompileException(exception, "Compilation task encountered an error"));
			List<CompilerDiagnostic> diagnostics = result.getDiagnostics();
			if (!diagnostics.isEmpty())
				return new ExpressionResult(remap(code, diagnostics));
		}
		byte[] klass = result.getCompilations().get(className);
		if (klass == null)
			return new ExpressionResult(new ExpressionCompileException("Compilation results missing the generated expression class"));

		// Convert the compiled class to JASM
		try {
			PrintContext<?> context = new PrintContext<>(assemblerConfig.getDisassemblyIndent().getValue());
			JvmClassPrinter printer = new JvmClassPrinter(new ByteArrayInputStream(klass));
			JvmMethodPrinter method = (JvmMethodPrinter) printer.method(methodName, methodDescriptorWithVariables());
			if (method == null)
				return new ExpressionResult(new ExpressionCompileException("Target method was not in generated class"));
			method.setLabelPrefix("g");
			method.print(context);
			return new ExpressionResult(context.toString());
		} catch (IOException ex) {
			return new ExpressionResult(new ExpressionCompileException(ex, "Failed to print generated class"));
		} catch (ExpressionCompileException ex) {
			return new ExpressionResult(ex);
		}
	}

	/**
	 * @param expression
	 * 		Expression to compile.
	 *
	 * @return Generated class to pass to {@code javac} for full-class compilation.
	 *
	 * @throws ExpressionCompileException
	 * 		When the class could not be fully generated.
	 */
	@Nonnull
	private String generateClass(@Nonnull String expression) throws ExpressionCompileException {
		StringBuilder code = new StringBuilder();

		// Append package
		if (className.indexOf('/') > 0) {
			String packageName = className.replace('/', '.').substring(0, className.lastIndexOf('/'));
			code.append("package ").append(packageName).append(";\n");
		}

		// Add imports from the user defined expression.
		// Remove the imports from the expression once copied to the output code.
		StringBuilder expressionBuffer = new StringBuilder();
		expression.lines().forEach(l -> {
			Matcher matcher = IMPORT_EXTRACT_PATTERN.matcher(l);
			if (matcher.find()) {
				code.append(matcher.group(1)).append('\n');
			} else {
				expressionBuffer.append(l).append('\n');
			}
		});
		expression = expressionBuffer.toString();

		// Class structure
		boolean isEnum = AccessFlag.isEnum(classAccess);
		code.append(isEnum ? "enum " : "abstract class ").append(StringUtil.shortenPath(className));
		if (superName != null && !superName.equals("java/lang/Object") && !superName.equals("java/lang/Enum"))
			code.append(" extends ").append(superName.replace('/', '.'));
		if (implementing != null && !implementing.isEmpty())
			code.append(" implements ").append(implementing.stream().map(s -> s.replace('/', '.')).collect(Collectors.joining(", "))).append(' ');
		code.append("{\n");

		// Enum constants must come first if the class is an enum.
		if (isEnum) {
			int enumConsts = 0;
			for (FieldMember field : fields) {
				if (field.getDescriptor().length() == 1)
					continue;
				InstanceType fieldDesc = Types.instanceTypeFromDescriptor(field.getDescriptor());
				if (fieldDesc.internalName().equals(className) && field.hasFinalModifier() && field.hasStaticModifier()) {
					if (enumConsts > 0)
						code.append(", ");
					code.append(field.getName());
					enumConsts++;
				}
			}
			code.append(';');
		}

		// Need to build the method structure to house the expression.
		// We'll start off with the access level.
		int parameterVarIndex = 0;
		if (AccessFlag.isPublic(methodFlags))
			code.append("public ");
		else if (AccessFlag.isProtected(methodFlags))
			code.append("protected ");
		else if (AccessFlag.isPrivate(methodFlags))
			code.append("private ");
		if (AccessFlag.isStatic(methodFlags))
			code.append("static ");
		else
			parameterVarIndex++;

		// Add the return type.
		ClassType returnType = methodType.returnType();
		if (returnType instanceof PrimitiveType primitiveReturn) {
			code.append(primitiveReturn.name()).append(' ');
		} else if (returnType instanceof InstanceType instanceType) {
			code.append(instanceType.internalName().replace('/', '.')).append(' ');
		} else if (returnType instanceof ArrayType arrayReturn) {
			ClassType componentReturnType = arrayReturn.componentType();
			if (componentReturnType instanceof PrimitiveType primitiveReturn) {
				code.append(primitiveReturn.name());
			} else if (componentReturnType instanceof InstanceType instanceType) {
				code.append(instanceType.internalName().replace('/', '.'));
			}
			code.append("[]".repeat(arrayReturn.dimensions()));
		}

		// Now the method name.
		code.append(' ').append(methodName).append('(');

		// And now the parameters.
		int parameterCount = methodType.parameterTypes().size();
		Set<String> usedVariables = new HashSet<>();
		for (int i = 0; i < parameterCount; i++) {
			// Lookup the parameter variable
			LocalVariable parameterVariable = getParameterVariable(parameterVarIndex, i);
			String parameterName = parameterVariable.getName();

			// Record the parameter as being used
			usedVariables.add(parameterName);

			// Skip if the parameter is illegally named.
			if (!isSafeName(parameterName))
				continue;

			// Skip parameters with types that aren't accessible in the workspace.
			String descriptor = parameterVariable.getDescriptor();
			if (isMissingType(descriptor))
				continue;

			// Append the parameter.
			NameType varInfo = getInfo(parameterName, descriptor);
			parameterVarIndex += varInfo.size;
			code.append(varInfo.className).append(' ').append(varInfo.name);
			if (i < parameterCount - 1) code.append(", ");
		}
		for (LocalVariable variable : methodVariables) {
			String name = variable.getName();

			// Skip illegal named variables and the implicit 'this'
			if (!isSafeName(name) || name.equals("this"))
				continue;

			// Skip if we already included the parameter in the loop above.
			boolean hasPriorParameters = !usedVariables.isEmpty();
			if (!usedVariables.add(name))
				continue;

			// Skip parameters with types that aren't accessible in the workspace.
			String descriptor = variable.getDescriptor();
			if (isMissingType(descriptor))
				continue;

			// Append the parameter.
			NameType varInfo = getInfo(name, descriptor);
			if (hasPriorParameters)
				code.append(", ");
			code.append(varInfo.className).append(' ').append(varInfo.name);
		}

		// If we skipped the last parameter for some reason we need to remove the trailing ', ' before closing
		// off the parameters section.
		if (code.substring(code.length() - 2).endsWith(", "))
			code.setLength(code.length() - 2);

		// Close off declaration and add a throws so the user doesn't need to specify try-catch.
		code.append(") throws Throwable { " + EXPR_MARKER + " \n");
		code.append(expression);
		code.append("}\n");

		// Stub out fields / methods
		for (FieldMember field : fields) {
			// Skip stubbing compiler-generated fields.
			if (field.hasBridgeModifier() || field.hasSyntheticModifier())
				continue;

			// Skip stubbing of illegally named fields.
			String name = field.getName();
			if (!isSafeName(name))
				continue;
			NameType fieldNameType = getInfo(name, field.getDescriptor());
			if (!isSafeClassName(fieldNameType.className))
				continue;

			// Skip enum constants, we added those earlier.
			if (fieldNameType.className.equals(className.replace('/', '.')) && field.hasFinalModifier() && field.hasStaticModifier())
				continue;

			// Skip fields with types that aren't accessible in the workspace.
			if (isMissingType(field.getDescriptor())) continue;

			// Append the field. The only modifier that we care about here is if it is static or not.
			if (field.hasStaticModifier())
				code.append("static ");
			code.append(fieldNameType.className).append(' ').append(fieldNameType.name).append(";\n");
		}
		for (MethodMember method : methods) {
			// Skip stubbing compiler-generated methods.
			if (method.hasBridgeModifier() || method.hasSyntheticModifier())
				continue;

			// Skip stubbing of illegally named methods.
			String name = method.getName();
			boolean isCtor = false;
			if (name.equals("<init>")) {
				if (isEnum) // Skip constructors for enum classes since we always drop enum const parameters.
					continue;
				isCtor = true;
			} else if (!isSafeName(name))
				continue;

			// Skip stubbing the method if it is the one we're assembling the expression within.
			String descriptor = method.getDescriptor();
			MethodType localMethodType = Types.methodType(descriptor);
			if (methodName.equals(name) && methodType.equals(localMethodType))
				continue;

			// Skip enum's 'valueOf'
			if (isEnum &&
					name.equals("valueOf") &&
					descriptor.equals("(Ljava/lang/String;)L" + className + ";"))
				continue;

			// Skip stubbing of methods with bad return types / bad parameter types.
			NameType returnInfo = getInfo(name, localMethodType.returnType().descriptor());
			if (!isSafeClassName(returnInfo.className))
				continue;
			List<ClassType> parameterTypes = localMethodType.parameterTypes();
			if (!parameterTypes.stream().map(p -> {
				try {
					return getInfo("p", p.descriptor()).className();
				} catch (Throwable t) {
					return "\0"; // Bogus which will throw off the safe name check.
				}
			}).allMatch(ExpressionCompiler::isSafeClassName))
				continue;

			// Skip methods with return/parameter types that aren't accessible in the workspace.
			boolean hasMissingType = false;
			Type[] types = new Type[parameterTypes.size() + 1];
			for (int i = 0; i < types.length - 1; i++)
				types[i] = parameterTypes.get(i);
			types[parameterTypes.size()] = localMethodType.returnType();
			for (Type type : types) {
				hasMissingType = isMissingType(type);
				if (hasMissingType)
					break;
			}
			if (hasMissingType) continue;

			// Stub the method. Start with the access modifiers.
			if (method.hasPublicModifier())
				code.append("public ");
			else if (method.hasProtectedModifier())
				code.append("protected ");
			else if (method.hasPrivateModifier())
				code.append("private ");
			if (method.hasStaticModifier())
				code.append("static ");

			// Method name. Consider edge case for constructors.
			if (isCtor)
				code.append(StringUtil.shortenPath(className)).append('(');
			else
				code.append(returnInfo.className).append(' ').append(returnInfo.name).append('(');

			// Add the parameters. We only care about the types, names don't really matter.
			List<ClassType> methodParameterTypes = parameterTypes;
			parameterCount = methodParameterTypes.size();
			for (int i = 0; i < parameterCount; i++) {
				ClassType paramType = methodParameterTypes.get(i);
				NameType paramInfo = getInfo("p" + i, paramType.descriptor());
				code.append(paramInfo.className).append(' ').append(paramInfo.name);
				if (i < parameterCount - 1) code.append(", ");
			}
			code.append(") { ");
			if (isCtor) {
				// If we know the parent type, we need to properly implement the constructor.
				// If we don't know the parent type, we cannot generate a valid constructor.
				ClassPathNode superPath = superName == null ? null : workspace.findJvmClass(superName);
				if (superPath == null && superName != null)
					throw new ExpressionCompileException("Cannot generate 'super(...)' for constructor, " +
							"missing type information for: " + superName);
				if (superPath != null) {
					// To make it easy, we'll find the simplest constructor in the parent class and pass dummy values.
					// Unlike regular methods we cannot just say 'throw new RuntimeException();' since calling
					// the 'super(...)' is required.
					MethodType parentConstructor = superPath.getValue().methodStream()
							.filter(m -> m.getName().equals("<init>"))
							.map(m -> Types.methodType(m.getDescriptor()))
							.min(Comparator.comparingInt(a -> a.parameterTypes().size()))
							.orElse(null);
					if (parentConstructor != null) {
						code.append("super(");
						parameterCount = parentConstructor.parameterTypes().size();
						for (int i = 0; i < parameterCount; i++) {
							ClassType type = parentConstructor.parameterTypes().get(i);
							if (type instanceof ObjectType) {
								code.append("null");
							} else {
								char prim = type.descriptor().charAt(0);
								if (prim == 'Z')
									code.append("false");
								else
									code.append('0');
							}
							if (i < parameterCount - 1) code.append(", ");
						}
						code.append(");");
					}
				}
			} else {
				code.append("throw new RuntimeException();");
			}
			code.append(" }\n");
		}

		// Done with the class
		code.append("}\n");
		return code.toString();
	}

	/**
	 * @param name
	 * 		Variable name.
	 * @param descriptor
	 * 		Variable descriptor.
	 *
	 * @return Variable info wrapper.
	 *
	 * @throws ExpressionCompileException
	 * 		When the variable descriptor is malformed.
	 */
	@Nonnull
	private NameType getInfo(@Nonnull String name, @Nonnull String descriptor) throws ExpressionCompileException {
		int size;
		String className;
		if (Types.isPrimitive(descriptor)) {
			PrimitiveType primitiveType = Types.primitiveFromDesc(descriptor);
			size = Types.category(primitiveType);
			className = primitiveType.name();
		} else if (descriptor.charAt(0) == '[') {
			ArrayType arrayParameterType = Types.arrayTypeFromDescriptor(descriptor);
			ClassType componentReturnType = arrayParameterType.componentType();
			if (componentReturnType instanceof PrimitiveType primitiveParameter) {
				className = primitiveParameter.name();
			} else if (componentReturnType instanceof InstanceType instanceType) {
				className = instanceType.internalName().replace('/', '.').replace('$', '.');
			} else {
				throw new ExpressionCompileException("Illegal component type: " + componentReturnType);
			}
			className += "[]".repeat(arrayParameterType.dimensions());
			size = 1;
		} else {
			size = 1;
			className = Types.instanceTypeFromDescriptor(descriptor).internalName().replace('/', '.').replace('$', '.');
		}
		return new NameType(size, name, className);
	}

	/**
	 * @param index
	 * 		Local variable index.
	 *
	 * @return Variable entry from the target method, or {@code null} if not known.
	 */
	@Nullable
	private LocalVariable findVar(int index) {
		if (methodVariables == null) return null;
		return methodVariables.stream()
				.filter(l -> l.getIndex() == index)
				.findFirst().orElse(null);
	}

	/**
	 * @param parameterVarIndex
	 * 		Local variable index of the parameter.
	 * @param parameterIndex
	 * 		Parameter index.
	 *
	 * @return Local variable info of the parameter.
	 */
	@Nonnull
	private LocalVariable getParameterVariable(int parameterVarIndex, int parameterIndex) {
		LocalVariable parameterVariable = findVar(parameterVarIndex);
		if (parameterVariable == null) {
			List<ClassType> parameterTypes = methodType.parameterTypes();
			ClassType parameterType;
			if (parameterIndex < parameterTypes.size()) {
				parameterType = parameterTypes.get(parameterIndex);
			} else {
				logger.warn("Could not resolve parameter variable (pVar={}, pIndex={}) in {}", parameterVarIndex, parameterIndex, methodName);
				parameterType = Types.OBJECT;
			}
			parameterVariable = new BasicLocalVariable(parameterVarIndex, "p" + parameterIndex, parameterType.descriptor(), null);

		}
		return parameterVariable;
	}

	/**
	 * @param code
	 * 		Generateed code to work with.
	 * @param diagnostics
	 * 		Compiler diagnostics affecting the given code.
	 *
	 * @return Diagnostics mapped to the original expression lines, rather than the lines in the full generated code.
	 */
	@Nonnull
	private static List<CompilerDiagnostic> remap(@Nonnull String code, @Nonnull List<CompilerDiagnostic> diagnostics) {
		// Given the following example code:
		//
		// 1:  package foo;
		// 2:  class Foo extends Bar {
		// 3:  void method() { /* EXPR_START */
		// 4:    // Code here
		//
		// The expression marker is on line 3, and our code starts on line four. So the reported line numbers need to
		// be shifted down by three. There are two line breaks between the start and the marker, so we add plus one
		// to consider the line the marker is itself on.
		int exprStart = code.indexOf(EXPR_MARKER);
		int lineOffset = StringUtil.count('\n', code.substring(0, exprStart)) + 1;
		return diagnostics.stream()
				.map(d -> d.withLine(d.line() - lineOffset))
				.toList();
	}

	/**
	 * <b>Note</b>: The logic for appending parameters to the desc within this method must align with {@link #generateClass(String)}.
	 *
	 * @return The method descriptor with additional parameters from the {@link #methodVariables} appended at the end.
	 *
	 * @throws ExpressionCompileException
	 * 		When parameter variable information cannot be found.
	 */
	@Nonnull
	private String methodDescriptorWithVariables() throws ExpressionCompileException {
		StringBuilder sb = new StringBuilder("(");
		int parameterVarIndex = AccessFlag.isStatic(methodFlags) ? 0 : 1;
		int parameterCount = methodType.parameterTypes().size();
		Set<String> usedVariables = new HashSet<>();
		for (int i = 0; i < parameterCount; i++) {
			LocalVariable parameterVariable = getParameterVariable(parameterVarIndex, i);
			String parameterName = parameterVariable.getName();
			usedVariables.add(parameterName);
			if (!isSafeName(parameterName))
				continue;
			String descriptor = parameterVariable.getDescriptor();
			if (isMissingType(descriptor))
				continue;
			NameType varInfo = getInfo(parameterName, descriptor);
			parameterVarIndex += varInfo.size;
			sb.append(descriptor);
		}
		for (LocalVariable variable : methodVariables) {
			String name = variable.getName();
			if (!isSafeName(name) || name.equals("this"))
				continue;
			if (!usedVariables.add(name))
				continue;
			String descriptor = variable.getDescriptor();
			if (isMissingType(descriptor))
				continue;
			sb.append(descriptor);
		}
		sb.append(')').append(methodType.returnType().descriptor());
		return sb.toString();
	}

	/**
	 * @param descriptor
	 * 		Some non-method descriptor.
	 *
	 * @return {@code true} if the type in the descriptor is found in the {@link #workspace}.
	 */
	private boolean isMissingType(@Nonnull String descriptor) {
		Type type = Types.typeFromDescriptor(descriptor);
		return isMissingType(type);
	}

	/**
	 * @param type
	 * 		Some non-method type.
	 *
	 * @return {@code true} if the type in the descriptor is found in the {@link #workspace}.
	 */
	private boolean isMissingType(@Nonnull Type type) {
		if (type instanceof InstanceType instanceType && workspace.findClass(instanceType.internalName()) == null)
			return true;
		else
			return type instanceof ArrayType arrayType
					&& arrayType.rootComponentType() instanceof InstanceType instanceType
					&& workspace.findClass(instanceType.internalName()) == null;
	}

	/**
	 * @param name
	 * 		Name to check.
	 *
	 * @return {@code true} when it can be used as a variable name safely.
	 */
	private static boolean isSafeName(@Nonnull String name) {
		// Name must not be empty.
		if (name.isEmpty())
			return false;

		// Must be comprised of valid identifier characters.
		char first = name.charAt(0);
		if (!Character.isJavaIdentifierStart(first))
			return false;
		char[] chars = name.toCharArray();
		for (int i = 1; i < chars.length; i++) {
			if (!Character.isJavaIdentifierPart(chars[i]))
				return false;
		}

		// Cannot be a reserved keyword.
		return !Keywords.getKeywords().contains(name);
	}

	/**
	 * @param internalName
	 * 		Name to check. Expected to be in the internal format. IE {@code java/lang/String}.
	 *
	 * @return {@code true} when it can be used as a class name safely.
	 */
	private static boolean isSafeInternalClassName(@Nonnull String internalName) {
		// Sanity check input
		if (internalName.indexOf('.') >= 0)
			throw new IllegalStateException("Saw source name format, expected internal name format");

		// All package name portions and the class name must be valid names.
		return StringUtil.fastSplit(internalName, true, '/').stream()
				.allMatch(ExpressionCompiler::isSafeName);
	}

	/**
	 * @param name
	 * 		Name to check. Expected to be in the source format. IE {@code java.lang.String}.
	 *
	 * @return {@code true} when it can be used as a class name safely.
	 */
	private static boolean isSafeClassName(@Nonnull String name) {
		// Sanity check input
		if (name.indexOf('/') >= 0)
			throw new IllegalStateException("Saw internal name format, expected source name format");

		// Allow primitives
		if (software.coley.recaf.util.Types.isPrimitiveClassName(name))
			return true;

		// All package name portions and the class name must be valid names.
		return StringUtil.fastSplit(name, true, '.').stream()
				.allMatch(ExpressionCompiler::isSafeName);
	}

	/**
	 * Wrapper for field/variable info.
	 *
	 * @param size
	 * 		Variable slot size.
	 * @param name
	 * 		Variable name.
	 * @param className
	 * 		Variable class type name.
	 */
	private record NameType(int size, @Nonnull String name, @Nonnull String className) {
	}
}
