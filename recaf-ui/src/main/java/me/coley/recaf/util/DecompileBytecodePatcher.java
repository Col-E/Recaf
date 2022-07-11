package me.coley.recaf.util;

import me.coley.recaf.Controller;
import me.coley.recaf.config.Configs;
import me.coley.recaf.config.container.DecompilerConfig;
import me.coley.recaf.decompile.PreDecompileInterceptor;
import me.coley.recaf.decompile.Decompiler;
import me.coley.recaf.util.visitor.SignatureRemovingVisitor;
import me.coley.recaf.util.visitor.SyntheticRemovingVisitor;
import me.coley.recaf.util.visitor.VariableRemovingVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

/**
 * Filters decompiler input.
 *
 * @author Matt Coley
 */
public class DecompileBytecodePatcher {
	private static final PreDecompileInterceptor interceptor = DecompileBytecodePatcher::filter;

	/**
	 * Registers a {@link PreDecompileInterceptor} that modifies bytecode
	 * according to config in {@link DecompilerConfig}.
	 *
	 * @param controller
	 * 		Controller access to the {@link me.coley.recaf.decompile.DecompileManager}.
	 */
	public static void install(Controller controller) {
		for (Decompiler decompiler : controller.getServices().getDecompileManager().getRegisteredImpls()) {
			decompiler.addPreDecompileInterceptor(interceptor);
		}
	}

	private static byte[] filter(byte[] code) {
		DecompilerConfig conf = Configs.decompiler();
		boolean modified = false;
		ClassWriter cw = new ClassWriter(0);
		ClassVisitor cv = cw;
		if (conf.filterGenerics) {
			cv = new SignatureRemovingVisitor(cv);
			modified = true;
		}
		if (conf.filterVars) {
			cv = new VariableRemovingVisitor(cv);
			modified = true;
		}
		if (conf.filterSynthetics) {
			cv = new SyntheticRemovingVisitor(cv);
			modified = true;
		}
		if (modified) {
			ClassReader cr = new ClassReader(code);
			cr.accept(cv, 0);
			return cw.toByteArray();
		}
		return code;
	}
}
