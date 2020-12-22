package me.coley.recaf.compiler;

import javassist.*;
import javassist.bytecode.LocalVariableAttribute;
import javassist.compiler.*;
import me.coley.recaf.parse.bytecode.VariableNameCache;
import org.objectweb.asm.tree.LocalVariableNode;

import java.util.List;

/**
 * Javassist compiler utility.
 *
 * @author Matt
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
	 * @param expression
	 * 		Source of the expression.
	 * @param existingVars
	 * 		Variable information to populate.
	 * @param varCache
	 * Variable name and index information.
	 *
	 * @return Compiled expression.
	 *
	 * @throws CannotCompileException
	 * 		When a compilation error occurred.
	 */
	public static JavassistCompilationResult compileExpression(CtClass declaring, CtBehavior containerMethod,
															   String expression, List<LocalVariableNode> existingVars,
															   VariableNameCache varCache)
			throws CannotCompileException {
		try {
			JavassistExpressionJavac compiler = new JavassistExpressionJavac(declaring, varCache);
			populateVariables(compiler, existingVars);
			populateVariables(compiler, containerMethod);
			compiler.compileStmnt(expression);
			return new JavassistCompilationResult(compiler.getGeneratedBytecode(), compiler.getLastCompiledSymbols());
		} catch (CompileError e) {
			throw new CannotCompileException(e);
		}
	}

	private static void populateVariables(JavassistExpressionJavac compiler, List<LocalVariableNode> variables) {
		JvstCodeGen gen = compiler.getGen();
		SymbolTable symbolTable = compiler.getRootSTable();
		for (LocalVariableNode variable : variables) {
			try {
				gen.recordVariable(variable.desc, variable.name, variable.index, symbolTable);
			} catch (CompileError ignored) {
				// ignored
			}
		}
	}

	private static void populateVariables(JavassistExpressionJavac compiler, CtBehavior containerMethod) {
		LocalVariableAttribute variables = (LocalVariableAttribute)
				containerMethod.getMethodInfo().getCodeAttribute().getAttribute(LocalVariableAttribute.tag);
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
