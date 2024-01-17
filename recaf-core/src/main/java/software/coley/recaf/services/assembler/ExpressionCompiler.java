package software.coley.recaf.services.assembler;

import dev.xdark.blw.type.*;
import dev.xdark.blw.type.Types;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import me.darknet.assembler.printer.JvmClassPrinter;
import me.darknet.assembler.printer.MethodPrinter;
import me.darknet.assembler.printer.PrintContext;
import org.objectweb.asm.Opcodes;
import regexodus.Matcher;
import regexodus.Pattern;
import software.coley.recaf.cdi.WorkspaceScoped;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.LocalVariable;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.compile.CompilerDiagnostic;
import software.coley.recaf.services.compile.CompilerResult;
import software.coley.recaf.services.compile.JavacArguments;
import software.coley.recaf.services.compile.JavacCompiler;
import software.coley.recaf.util.*;
import software.coley.recaf.workspace.model.Workspace;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Compiles Java source expressions into JASM.
 *
 * @author Matt Coley
 */
@WorkspaceScoped
public class ExpressionCompiler {
	private static final Pattern IMPORT_EXTRACT_PATTERN = RegexUtil.pattern("^\\s*(import \\w.+;)");
	private static final Pattern WORD_PATTERN = RegexUtil.pattern("\\w+");
	private static final Pattern WORD_DOT_PATTERN = RegexUtil.pattern("[\\w.]+");
	private static final String EXPR_MARKER = "/* EXPR_START */";
	private final JavacCompiler javac;
	private final Workspace workspace;
	private final AssemblerPipelineGeneralConfig assemblerConfig;
	private String className;
	private int classAccess;
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
		className = classInfo.getName();
		classAccess = classInfo.getAccess();
		versionTarget = NumberUtil.intClamp(classInfo.getVersion() - JvmClassInfo.BASE_VERSION, JavacCompiler.getMinTargetVersion(), JavaVersion.get());
		superName = classInfo.getSuperName();
		implementing = classInfo.getInterfaces();
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
		JavacArguments arguments = new JavacArguments(className, code, null, versionTarget, -1, true, false, false);
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
			MethodPrinter method = printer.method(methodName, methodDescriptorWithVariables());
			if (method == null)
				return new ExpressionResult(new ExpressionCompileException("Target method was not in generated class"));
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

		// TODO: Different model for enums
		// TODO: If the method type ends in a return value, we need to have a dummy return at the end

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
		code.append(AccessFlag.isEnum(classAccess) ? "enum " : "abstract class ").append(StringUtil.shortenPath(className));
		if (superName != null && !superName.equals("java/lang/Object") && !superName.equals("java/lang/Enum"))
			code.append(" extends ").append(superName.replace('/', '.'));
		if (implementing != null && !implementing.isEmpty())
			code.append(" implements ").append(implementing.stream().map(s -> s.replace('/', '.')).collect(Collectors.joining(", "))).append(' ');
		code.append("{\n");

		// Enum constants must come first if the class is an enum
		if (AccessFlag.isEnum(classAccess)) {
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

		// Method structure to house the expression
		int parameterVarIndex = 0;
		if (AccessFlag.isStatic(methodFlags))
			code.append("static ");
		else
			parameterVarIndex++;
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
		code.append(' ').append(methodName).append('(');
		int parameterCount = methodType.parameterTypes().size();
		Set<String> usedVariables = new HashSet<>();
		for (int i = 0; i < parameterCount; i++) {
			LocalVariable parameterVariable = findVar(parameterVarIndex);
			if (parameterVariable == null)
				throw new ExpressionCompileException("Missing variable at index " + parameterVarIndex);
			String parameterName = parameterVariable.getName();
			usedVariables.add(parameterName);
			NameType varInfo = getInfo(parameterName, parameterVariable.getDescriptor());
			parameterVarIndex += varInfo.size;
			code.append(varInfo.className).append(' ').append(varInfo.name);
			if (i < parameterCount - 1) code.append(", ");

		}
		for (LocalVariable variable : methodVariables) {
			String name = variable.getName();
			if (!isSafeName(name) || name.equals("this"))
				continue;
			boolean hasPriorParameters = !usedVariables.isEmpty();
			if (!usedVariables.add(name))
				continue;
			NameType varInfo = getInfo(name, variable.getDescriptor());
			if (hasPriorParameters)
				code.append(", ");
			code.append(varInfo.className).append(' ').append(varInfo.name);
		}
		code.append(") throws Throwable { " + EXPR_MARKER + " \n");
		code.append(expression);
		code.append("}\n");

		// Stub out fields / methods
		for (FieldMember field : fields) {
			// Skip stubbing of illegally named fields.
			String name = field.getName();
			if (!isSafeName(name))
				continue;
			NameType fieldInfo = getInfo(name, field.getDescriptor());
			if (!isSafeClassName(fieldInfo.className))
				continue;

			// Skip enum constants, we added those earlier.
			if (fieldInfo.className.equals(className.replace('/', '.')) && field.hasFinalModifier() && field.hasStaticModifier())
				continue;

			if (field.hasStaticModifier())
				code.append("static ");
			code.append(fieldInfo.className).append(' ').append(fieldInfo.name).append(";\n");
		}
		for (MethodMember method : methods) {
			// Skip stubbing of illegally named methods.
			String name = method.getName();
			if (!isSafeName(name))
				continue;

			// Skip stubbing the method if it is the one we're assembling the expression within.
			MethodType localMethodType = Types.methodType(method.getDescriptor());
			if (methodName.equals(name) && methodType.equals(localMethodType))
				continue;

			// Skip enum's 'valueOf'
			if (AccessFlag.isEnum(classAccess) &&
					name.equals("valueOf") &&
					method.getDescriptor().equals("(Ljava/lang/String;)L" + className + ";"))
				continue;

			// Skip stubbing of methods with bad return types / bad parameter types.
			NameType returnInfo = getInfo(name, localMethodType.returnType().descriptor());
			if (!isSafeClassName(returnInfo.className))
				continue;
			if (!localMethodType.parameterTypes().stream().map(p -> {
				try {
					return getInfo("p", p.descriptor()).className();
				} catch (Throwable t) {
					return "\0"; // Bogus which will throw off the safe name check.
				}
			}).allMatch(ExpressionCompiler::isSafeClassName))
				continue;

			// Stub the method
			if (method.hasStaticModifier())
				code.append("static ");
			code.append(returnInfo.className).append(' ').append(returnInfo.name).append('(');
			List<ClassType> methodParameterTypes = localMethodType.parameterTypes();
			parameterCount = methodParameterTypes.size();
			for (int i = 0; i < parameterCount; i++) {
				ClassType paramType = methodParameterTypes.get(i);
				NameType paramInfo = getInfo("p" + i, paramType.descriptor());
				code.append(paramInfo.className).append(' ').append(paramInfo.name);
				if (i < parameterCount - 1) code.append(", ");
			}
			code.append(") { throw new RuntimeException(); }\n");
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
				className = instanceType.internalName().replace('/', '.');
			} else {
				throw new ExpressionCompileException("Illegal component type: " + componentReturnType);
			}
			className += "[]".repeat(arrayParameterType.dimensions());
			size = 1;
		} else {
			size = 1;
			className = Types.instanceTypeFromDescriptor(descriptor).internalName().replace('/', '.');
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
	 * @param code
	 * 		Generateed code to work with.
	 * @param diagnostics
	 * 		Compiler diagnostics affecting the given code.
	 *
	 * @return Diagnostics mapped to the original expression lines, rather than the lines in the full generated code.
	 */
	@Nonnull
	private static List<CompilerDiagnostic> remap(@Nonnull String code, @Nonnull List<CompilerDiagnostic> diagnostics) {
		int exprStart = code.indexOf(EXPR_MARKER);
		int lineOffset = StringUtil.count('\n', code.substring(0, exprStart));
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
			LocalVariable parameterVariable = findVar(parameterVarIndex);
			if (parameterVariable == null)
				throw new ExpressionCompileException("Missing variable at index " + parameterVarIndex);
			String parameterName = parameterVariable.getName();
			usedVariables.add(parameterName);
			NameType varInfo = getInfo(parameterName, parameterVariable.getDescriptor());
			parameterVarIndex += varInfo.size;
			sb.append(parameterVariable.getDescriptor());
		}
		for (LocalVariable variable : methodVariables) {
			String name = variable.getName();
			if (!isSafeName(name) || name.equals("this"))
				continue;
			if (!usedVariables.add(name))
				continue;
			sb.append(variable.getDescriptor());
		}
		sb.append(')').append(methodType.returnType().descriptor());
		return sb.toString();
	}

	/**
	 * @param name
	 * 		Name to check.
	 *
	 * @return {@code true} when it can be used as a variable name safely.
	 */
	private static boolean isSafeName(@Nonnull String name) {
		return WORD_PATTERN.matches(name);
	}

	/**
	 * @param name
	 * 		Name to check.
	 *
	 * @return {@code true} when it can be used as a class name safely.
	 */
	private static boolean isSafeClassName(@Nonnull String name) {
		return WORD_DOT_PATTERN.matches(name);
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
