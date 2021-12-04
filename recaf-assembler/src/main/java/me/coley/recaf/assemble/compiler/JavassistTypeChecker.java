package me.coley.recaf.assemble.compiler;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.compiler.JvstTypeChecker;

/**
 * Modified type checker for Javassist to pull information from Recaf.
 *
 * @author Matt Coley
 */
public class JavassistTypeChecker extends JvstTypeChecker {
	/**
	 * @param workspace
	 * 		Workspace to pull class information from.
	 * @param declaringClass
	 * 		Class containing the method our expression resides in.
	 * @param classPool
	 * 		Class pool to use.
	 * @param javassistCodeGen
	 * 		Parent code generator context.
	 */
	public JavassistTypeChecker(ClassSupplier workspace, CtClass declaringClass, ClassPool classPool,
								JavassistCodeGen javassistCodeGen) {
		super(declaringClass, classPool, javassistCodeGen);
		this.resolver = new JavassistMemberResolver(workspace, classPool);
	}
}
