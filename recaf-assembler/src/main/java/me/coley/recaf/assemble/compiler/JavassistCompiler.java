package me.coley.recaf.assemble.compiler;

import javassist.*;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.compiler.CompileError;
import javassist.compiler.Javac;
import javassist.compiler.JvstCodeGen;
import javassist.compiler.SymbolTable;
import me.coley.recaf.assemble.util.ClassSupplier;
import me.coley.recaf.assemble.ast.meta.Expression;
import me.coley.recaf.assemble.transformer.VariableInfo;
import me.coley.recaf.assemble.transformer.Variables;

/**
 * Javassist compiler utility.
 *
 * @author Matt Coley
 */
public class JavassistCompiler {
	/**
	 * Compile an independent method body.
	 *
	 * @param declaring
	 * 		Declaring type that contains the method.
	 * @param src
	 * 		Source of the method declaration.
	 *
	 * @return Compiled method.
	 *
	 * @throws CannotCompileException
	 * 		When a compilation error occurred.
	 */
	public static CtMethod compileMethod(CtClass declaring, String src) throws CannotCompileException {
		try {
			Javac compiler = new Javac(declaring);
			CtMember obj = compiler.compile(src);
			if (obj instanceof CtMethod)
				return (CtMethod) obj;
		} catch (CompileError e) {
			throw new CannotCompileException(e);
		}
		throw new CannotCompileException("Not a method");
	}

	/**
	 * Compile an independent method body.
	 *
	 * @param declaring
	 * 		Declaring type that contains the method.
	 * @param containerMethod
	 * 		Declaring method that will contain the expression.
	 * @param classSupplier
	 * 		Class information supplier.
	 * @param expression
	 * 		Source of the expression.
	 * @param variables
	 * 		Variable name and index information.
	 * @param isStatic
	 * 		Flag for compiler.
	 *
	 * @return Compiled expression.
	 *
	 * @throws CannotCompileException
	 * 		When a compilation error occurred.
	 */
	public static JavassistCompilationResult compileExpression(CtClass declaring, CtBehavior containerMethod,
															   ClassSupplier classSupplier,
															   Expression expression,
															   Variables variables,
															   boolean isStatic)
			throws CannotCompileException {
		try {
			if (variables == null)
				throw new CannotCompileException("Recaf variables instance is nul");
			JavassistExpressionJavac compiler
					= new JavassistExpressionJavac(declaring, classSupplier, variables, expression, isStatic);
			populateVariables(compiler, variables);
			populateVariables(compiler, containerMethod);
			compiler.compileStmnt(expression.getCode());
			return new JavassistCompilationResult(compiler.getGeneratedBytecode(), compiler.getLastCompiledSymbols());
		} catch (CompileError e) {
			throw new CannotCompileException(e);
		}
	}

	private static void populateVariables(JavassistExpressionJavac compiler, Variables variables) {
		JvstCodeGen gen = compiler.getGen();
		SymbolTable symbolTable = compiler.getRootSTable();
		// NOTE: Population order really matters here. In our case appearance order satisfies most cases.
		// Since we track parameters first they will always take preference in edge cases with scoping.
		for (VariableInfo variable : variables.inAppearanceOrder()) {
			try {
				String name = variable.getName();
				String desc = variable.getLastUsedType().getDescriptor();
				int index = variable.getIndex();
				gen.recordVariable(desc, name, index, symbolTable);
			} catch (CompileError ignored) {
				// ignored
			}
		}
	}

	private static void populateVariables(JavassistExpressionJavac compiler, CtBehavior containerMethod) {
		CodeAttribute code = containerMethod.getMethodInfo().getCodeAttribute();
		if (code == null)
			return;
		LocalVariableAttribute variables = (LocalVariableAttribute) code.getAttribute(LocalVariableAttribute.tag);
		if (variables != null) {
			JvstCodeGen gen = compiler.getGen();
			SymbolTable symbolTable = compiler.getRootSTable();
			for (int i = 0; i < variables.tableLength(); i++) {
				int index = variables.index(i);
				String signature = variables.signature(i);
				String name = variables.variableName(i);
				try {
					gen.recordVariable(signature, name, index, symbolTable);
				} catch (CompileError ignored) {
					// ignored
				}
			}
		}
	}
}
