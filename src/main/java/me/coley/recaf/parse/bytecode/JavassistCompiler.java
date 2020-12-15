package me.coley.recaf.parse.bytecode;

import javassist.*;
import javassist.bytecode.Bytecode;
import javassist.bytecode.LocalVariableAttribute;
import javassist.compiler.CompileError;
import javassist.compiler.Javac;
import javassist.compiler.JvstCodeGen;
import javassist.compiler.SymbolTable;

import java.lang.reflect.Field;

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
	 *
	 * @return Compiled method.
	 *
	 * @throws CannotCompileException
	 * 		When a compilation error occurred.
	 */
	public static Bytecode compileExpression(CtClass declaring, CtBehavior containerMethod, String expression)
			throws CannotCompileException {
		try {
			InternalJavac compiler = new InternalJavac(declaring);
			populateVariables(compiler, containerMethod);
			return compiler.compileBody(containerMethod, expression);
		} catch (CompileError e) {
			throw new CannotCompileException(e);
		}
	}

	private static void populateVariables(InternalJavac compiler, CtBehavior containerMethod) {
		LocalVariableAttribute variables = (LocalVariableAttribute)
				containerMethod.getMethodInfo().getCodeAttribute().getAttribute(LocalVariableAttribute.tag);
		if (variables != null) {
			JvstCodeGen gen = compiler.getGen();
			SymbolTable symbolTable = compiler.getSTable();
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

	/**
	 * An extension of Javassist's {@link Javac} that exposes some internal structures
	 * needed to properly inject local variable information.
	 *
	 * @author Matt
	 */
	private static class InternalJavac extends Javac {
		private static final Field fGen;
		private static final Field fSTable;

		public InternalJavac(CtClass declaring) {
			super(declaring);
		}

		public JvstCodeGen getGen() {
			try {
				return (JvstCodeGen) fGen.get(this);
			} catch (IllegalAccessException ex) {
				throw new IllegalStateException(ex);
			}
		}

		public SymbolTable getSTable() {
			try {
				return (SymbolTable) fSTable.get(this);
			} catch (IllegalAccessException ex) {
				throw new IllegalStateException(ex);
			}
		}

		static {
			try {
				fGen = Javac.class.getDeclaredField("gen");
				fGen.setAccessible(true);
				fSTable = Javac.class.getDeclaredField("stable");
				fSTable.setAccessible(true);
			} catch (ReflectiveOperationException ex) {
				throw new IllegalStateException(ex);
			}
		}
	}
}
