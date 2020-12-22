package me.coley.recaf.compiler;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.compiler.CompileError;
import javassist.compiler.MemberResolver;
import me.coley.recaf.workspace.Workspace;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Modified member resolver for Javassist to pull information from Recaf.
 *
 * @author Matt
 */
public class JavassistMemberResolver extends MemberResolver {
	private final Workspace workspace;

	/**
	 * @param workspace
	 * 		Workspace to pull class information from.
	 * @param classPool
	 * 		Class pool to use.
	 */
	public JavassistMemberResolver(Workspace workspace, ClassPool classPool) {
		super(classPool);
		this.workspace = workspace;
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
			byte[] clazz = workspace.getRawClass(name);
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
