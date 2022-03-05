package me.coley.recaf.assemble.compiler;

import javassist.*;
import javassist.compiler.CompileError;
import javassist.compiler.MemberResolver;
import me.coley.recaf.assemble.util.ClassSupplier;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Modified member resolver for Javassist to pull information from a class supplier.
 *
 * @author Matt Coley
 */
public class JavassistMemberResolver extends MemberResolver {
	private final ClassSupplier classSupplier;

	/**
	 * @param classSupplier
	 * 		Class information supplier.
	 * @param classPool
	 * 		Class pool to use.
	 */
	public JavassistMemberResolver(ClassSupplier classSupplier, ClassPool classPool) {
		super(classPool);
		this.classSupplier = classSupplier;
	}

	@Override
	public CtClass lookupClass(String name, boolean notCheckInner) throws CompileError {
		try {
			return super.lookupClass(name, notCheckInner);
		} catch (CompileError error) {
			return lookupFromSupplier(name.replace('.', '/'));
		}
	}

	private CtClass lookupFromSupplier(String name) throws CompileError {
		try {
			byte[] clazz = classSupplier.getClass(name);
			if (clazz == null)
				throw new IllegalArgumentException("Class does not exist");
			InputStream is = new ByteArrayInputStream(clazz);
			return getClassPool().makeClass(is);
		} catch (Exception ignored) {
			// If the class does not exist its ok to fail.
			String msg = ignored.getMessage();
			if (msg == null)
				msg = "'" + name + "' could not be found.";
			throw new CompileError(msg);
		}
	}
}
