package me.coley.recaf.assemble.compiler;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.compiler.CompileError;
import javassist.compiler.MemberResolver;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Modified member resolver for Javassist to pull information from Recaf.
 *
 * @author Matt Coley
 */
public class JavassistMemberResolver extends MemberResolver {
	private final ClassSupplier workspace;

	/**
	 * @param classSupplier
	 * 		Class information supplier.
	 * @param classPool
	 * 		Class pool to use.
	 */
	public JavassistMemberResolver(ClassSupplier classSupplier, ClassPool classPool) {
		super(classPool);
		this.workspace = classSupplier;
	}

	@Override
	public CtClass lookupClass(String name, boolean notCheckInner) throws CompileError {
		try {
			return super.lookupClass(name, notCheckInner);
		} catch (CompileError error) {
			return lookupWorkspaceClass(name.replace('.', '/'));
		}
	}

	private CtClass lookupWorkspaceClass(String name) throws CompileError {
		try {
			byte[] clazz = workspace.getClass(name);
			if (clazz == null)
				throw new IllegalArgumentException("Class does not exist in workspace");
			InputStream is = new ByteArrayInputStream(clazz);
			return getClassPool().makeClass(is);
		} catch (Exception ignored) {
			// If the class does not exist its ok to fail.
			String msg = ignored.getMessage();
			if (msg == null)
				msg = "'" + name + "' could not be found in workspace.\n" +
						"Add it as a library or enable class generation in the config.";
			throw new CompileError(msg);
		}
	}
}
