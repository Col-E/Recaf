package software.coley.recaf.services.assembler;

import dev.xdark.blw.type.MethodType;
import dev.xdark.blw.type.Types;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import me.darknet.assembler.printer.JvmClassPrinter;
import me.darknet.assembler.printer.JvmMethodPrinter;
import me.darknet.assembler.printer.PrintContext;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import regexodus.Pattern;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.LocalVariable;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.compile.CompilerDiagnostic;
import software.coley.recaf.services.compile.CompilerResult;
import software.coley.recaf.services.compile.JavacArguments;
import software.coley.recaf.services.compile.JavacCompiler;
import software.coley.recaf.services.compile.stub.ExpressionHostingClassStubGenerator;
import software.coley.recaf.util.AccessFlag;
import software.coley.recaf.util.JavaVersion;
import software.coley.recaf.util.NumberUtil;
import software.coley.recaf.util.RegexUtil;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.workspace.model.Workspace;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Compiles Java source expressions into JASM.
 *
 * @author Matt Coley
 */
@Dependent
public class ExpressionCompiler {
	private static final Logger logger = Logging.get(ExpressionCompiler.class);
	private static final Pattern IMPORT_EXTRACT_PATTERN = RegexUtil.pattern("^\\s*(import \\w.+;)");
	public static final String EXPR_MARKER = "/* EXPR_START */";
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
	private List<InnerClassInfo> innerClasses;
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
		innerClasses = Collections.emptyList();
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
		className = type;
		classAccess = classInfo.getAccess();
		versionTarget = NumberUtil.intClamp(classInfo.getVersion() - JvmClassInfo.BASE_VERSION, JavacCompiler.getMinTargetVersion(), JavaVersion.get());
		superName = classInfo.getSuperName();
		implementing = classInfo.getInterfaces();
		fields = classInfo.getFields();
		methods = classInfo.getMethods();
		innerClasses = classInfo.getInnerClasses();

		// We use bridge to denote that the default flags are set.
		// When we assign a class, there may be non-static fields/methods the user will want to interact with.
		// Thus, we should clear our flags from the default so that they can do that.
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
		methodName = method.getName();
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
		ExpressionHostingClassStubGenerator stubber;
		String code;
		try {
			stubber = new ExpressionHostingClassStubGenerator(workspace, classAccess, className, superName, implementing,
					fields, methods, innerClasses, methodFlags, methodName, methodType, methodVariables, expression);
			code = stubber.generate();
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
			JvmMethodPrinter method = (JvmMethodPrinter) printer.method(stubber.getAdaptedMethodName(), stubber.methodDescriptorWithVariables());
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
}
