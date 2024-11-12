package software.coley.recaf.services.compile.stub;

import dev.xdark.blw.type.ArrayType;
import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.InstanceType;
import dev.xdark.blw.type.MethodType;
import dev.xdark.blw.type.PrimitiveType;
import dev.xdark.blw.type.Types;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import regexodus.Matcher;
import regexodus.Pattern;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.info.member.BasicLocalVariable;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.LocalVariable;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.assembler.ExpressionCompileException;
import software.coley.recaf.services.assembler.ExpressionCompiler;
import software.coley.recaf.util.AccessFlag;
import software.coley.recaf.util.RegexUtil;
import software.coley.recaf.workspace.model.Workspace;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class stub generator which implements a specific method with a user-defined expression.
 *
 * @author Matt Coley
 * @see ExpressionCompiler#compile(String)
 */
public class ExpressionHostingClassStubGenerator extends ClassStubGenerator {
	private static final Logger logger = Logging.get(ExpressionHostingClassStubGenerator.class);
	private static final Pattern IMPORT_EXTRACT_PATTERN = RegexUtil.pattern("^\\s*(import \\w.+;)");
	private final int methodFlags;
	private final String methodName;
	private final MethodType methodType;
	private final List<LocalVariable> methodVariables;
	private final String expression;

	/**
	 * @param workspace
	 * 		Workspace to pull class information from.
	 * @param classAccess
	 * 		Host class access modifiers.
	 * @param className
	 * 		Host class name.
	 * @param superName
	 * 		Host class super name.
	 * @param implementing
	 * 		Host class interfaces implemented.
	 * @param fields
	 * 		Host class declared fields.
	 * @param methods
	 * 		Host class declared methods.
	 * @param innerClasses
	 * 		Host class declared inner classes.
	 * @param methodFlags
	 * 		Expression hosting method's access modifiers.
	 * @param methodName
	 * 		Expression hosting method's name.
	 * @param methodType
	 * 		Expression hosting method arguments + return type.
	 * @param methodVariables
	 * 		Expression hosting method's local variables.
	 * @param expression
	 * 		The expression to insert into the target hosting method.
	 */
	public ExpressionHostingClassStubGenerator(@Nonnull Workspace workspace,
	                                           int classAccess,
	                                           @Nonnull String className,
	                                           @Nullable String superName,
	                                           @Nonnull List<String> implementing,
	                                           @Nonnull List<FieldMember> fields,
	                                           @Nonnull List<MethodMember> methods,
	                                           @Nonnull List<InnerClassInfo> innerClasses,
	                                           int methodFlags,
	                                           @Nonnull String methodName,
	                                           @Nonnull MethodType methodType,
	                                           @Nonnull List<LocalVariable> methodVariables,
	                                           @Nonnull String expression) {
		super(workspace, classAccess, className, superName, implementing, fields, methods, innerClasses);

		// Map edge cases for disallowed names.
		if (methodName.equals("<init>"))
			methodName = "instance_ctor";
		else if (methodName.equals("<clinit>"))
			methodName = "static_ctor";
		else if (!isSafeName(methodName))
			methodName = "obfuscated_method";

		// Assign expression host method details
		this.methodFlags = methodFlags;
		this.methodName = methodName;
		this.methodType = methodType;
		this.methodVariables = methodVariables;
		this.expression = expression;
	}

	@Override
	public String generate() throws ExpressionCompileException {
		String localExpression = expression;

		StringBuilder code = new StringBuilder();
		appendPackage(code);
		localExpression = appendExpressionImports(code, localExpression);
		appendClassStructure(code);
		appendEnumConsts(code);
		appendExpressionMethod(code, localExpression);
		appendFields(code);
		appendMethods(code);
		appendInnerClasses(code);
		appendClassEnd(code);

		return code.toString();
	}

	@Override
	protected boolean doSkipMethod(@Nonnull String name, @Nonnull MethodType type) {
		// We want to skip generating a stub of the method our expression will reside within.
		return methodName.equals(name) && methodType.equals(type);
	}

	/**
	 * @return Adapted method name for compiler-safe use.
	 */
	@Nonnull
	public String getAdaptedMethodName() {
		return methodName;
	}

	/**
	 * Expressions can contain imports at the top so that the end-user can work without needing fully qualified names.
	 * We want to take those out and append them to the class we're generating, and update the expression to remove
	 * the imports so that we can slap it into the method body later without syntax issues coming from imports being
	 * used in a method body.
	 *
	 * @param code
	 * 		Class code to append imports to.
	 * @param expression
	 * 		Expression to extract imports from.
	 *
	 * @return Modified expression <i>(without imports)</i>
	 */
	@Nonnull
	private String appendExpressionImports(@Nonnull StringBuilder code, @Nonnull String expression) {
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
		return expressionBuffer.toString();
	}

	/**
	 * @param code
	 * 		Class code to append method definition to.
	 * @param expression
	 * 		User-defined expression.
	 *
	 * @throws ExpressionCompileException
	 * 		When the expression hosting method could not be fully generated.
	 */
	private void appendExpressionMethod(@Nonnull StringBuilder code, @Nonnull String expression) throws ExpressionCompileException {
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
			parameterVarIndex += varInfo.size();
			code.append(varInfo.className()).append(' ').append(varInfo.name());
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
			code.append(varInfo.className()).append(' ').append(varInfo.name());
		}

		// If we skipped the last parameter for some reason we need to remove the trailing ', ' before closing
		// off the parameters section.
		if (code.substring(code.length() - 2).endsWith(", "))
			code.setLength(code.length() - 2);

		// Close off declaration and add a throws so the user doesn't need to specify try-catch.
		code.append(") throws Throwable { " + ExpressionCompiler.EXPR_MARKER + " \n");
		code.append(expression);
		code.append("}\n");
	}

	/**
	 * <b>Note</b>: The logic for appending parameters to the desc within this method must align with {@link #generate()}.
	 *
	 * @return The method descriptor with additional parameters from the {@link #methodVariables} appended at the end.
	 *
	 * @throws ExpressionCompileException
	 * 		When parameter variable information cannot be found.
	 */
	@Nonnull
	public String methodDescriptorWithVariables() throws ExpressionCompileException {
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
			parameterVarIndex += varInfo.size();
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
}
