package me.coley.recaf.util;

import me.coley.recaf.Controller;
import me.coley.recaf.config.Configs;
import me.coley.recaf.config.container.DecompilerConfig;
import me.coley.recaf.decompile.Decompiler;
import me.coley.recaf.decompile.PostDecompileInterceptor;

/**
 * Filters decompiler output.
 *
 * @author Matt Coley
 */
public class DecompileTextPatcher {
	private static final PostDecompileInterceptor interceptor = DecompileTextPatcher::filter;

	/**
	 * Registers a {@link PostDecompileInterceptor} that modifies decompiled code
	 * according to config in {@link DecompilerConfig}.
	 *
	 * @param controller
	 * 		Controller access to the {@link me.coley.recaf.decompile.DecompileManager}.
	 */
	public static void install(Controller controller) {
		for (Decompiler decompiler : controller.getServices().getDecompileManager().getRegisteredImpls()) {
			decompiler.addPostDecompileInterceptor(interceptor);
		}
	}

	private static String filter(String code) {
		DecompilerConfig conf = Configs.decompiler();
		if (code != null && conf.escapeUnicode)
			code = EscapeUtil.unescapeUnicode(code);
		return code;
	}
}
