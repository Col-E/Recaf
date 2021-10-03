package me.coley.recaf.decompile.cfr;

import me.coley.recaf.decompile.DecompileInterceptor;
import me.coley.recaf.workspace.Workspace;
import org.benf.cfr.reader.api.ClassFileSource;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;

import java.util.Collection;
import java.util.Collections;

/**
 * CFR class source. Provides access to workspace clases.
 *
 * @author Matt
 */
public class ClassSource implements ClassFileSource {
	private final Workspace workspace;
	private final CfrDecompiler decompiler;

	/**
	 * Constructs a CFR class source.
	 *
	 * @param workspace
	 * 		Workspace to pull classes from.
	 * @param decompiler
	 * 		Decompiler instance.
	 */
	public ClassSource(Workspace workspace, CfrDecompiler decompiler) {
		this.workspace = workspace;
		this.decompiler = decompiler;
	}

	@Override
	public void informAnalysisRelativePathDetail(String usePath, String specPath) {
	}

	@Override
	public Collection<String> addJar(String jarPath) {
		return Collections.emptySet();
	}

	@Override
	public String getPossiblyRenamedPath(String path) {
		return path;
	}

	@Override
	public Pair<byte[], String> getClassFileContent(String inputPath) {
		String className = inputPath.substring(0, inputPath.indexOf(".class"));
		byte[] code = workspace.getResources().getClass(className).getValue();
		for (DecompileInterceptor interceptor : decompiler.getDecompileInterceptors()) {
			code = interceptor.apply(code);
		}
		return new Pair<>(code, inputPath);
	}

	/**
	 * @return Associated workspace of the class source.
	 */
	public Workspace getWorkspace() {
		return workspace;
	}
}
