package me.coley.recaf.decompile.cfr;

import me.coley.recaf.control.Controller;
import me.coley.recaf.util.ClassUtil;
import org.benf.cfr.reader.api.ClassFileSource;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;

import java.util.*;

/**
 * CFR class source. Provides access to workspace clases.
 *
 * @author Matt
 */
public class ClassSource implements ClassFileSource {
	private final Controller controller;

	/**
	 * Constructs a CFR class source.
	 *
	 * @param controller
	 * 		Controller with workspace to pull classes from.
	 */
	public ClassSource(Controller controller) {
		this.controller = controller;
	}

	@Override
	public void informAnalysisRelativePathDetail(String usePath, String specPath) {}

	@Override
	public Collection<String> addJar(String jarPath) {
		return Collections.emptySet();
	}

	@Override
	public String getPossiblyRenamedPath(String path) {
		return path;
	}

	@Override
	@SuppressWarnings("deprecation")
	public Pair<byte[], String> getClassFileContent(String inputPath) {
		String className = inputPath.substring(0, inputPath.indexOf(".class"));
		byte[] code = controller.getWorkspace().getRawClass(className);
		// Strip debug if config says so
		if (controller.config().decompile().stripDebug)
			code = ClassUtil.stripDebugForDecompile(code);
		// Fetch code from runtime if not in workspace
		if (code == null) {
			code = Objects.requireNonNull(ClassUtil.fromRuntime(className),
					"Failed to load class from runtime: " + className).b;
		}
		return new Pair<>(code, inputPath);
	}
}
