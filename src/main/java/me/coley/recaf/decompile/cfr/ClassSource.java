package me.coley.recaf.decompile.cfr;

import me.coley.recaf.util.ClasspathUtil;
import me.coley.recaf.workspace.Workspace;
import org.benf.cfr.reader.api.ClassFileSource;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

/**
 * CFR class source. Provides access to workspace clases.
 *
 * @author Matt
 */
public class ClassSource implements ClassFileSource {
	private final Workspace workspace;

	/**
	 * Constructs a CFR class source.
	 *
	 * @param workspace
	 * 		Workspace to pull classes from.
	 */
	public ClassSource(Workspace workspace) {
		this.workspace = workspace;
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
	@SuppressWarnings("deprecated")
	public Pair<byte[], String> getClassFileContent(String inputPath) throws IOException {
		String className = inputPath.substring(0, inputPath.indexOf(".class"));
		byte[] code;
		if(workspace.hasClass(className))
			code = workspace.getRawClass(className);
		else
			code = fromRuntime(className).b;
		return new Pair<>(code, inputPath);
	}

	// TODO: This is copied from ClassVertex, move this to a util class.

	/**
	 * @param name
	 * 		Internal class name.
	 *
	 * @return {@link org.objectweb.asm.ClassReader} loaded from runtime.
	 */
	private static ClassReader fromRuntime(String name) {
		try {
			Class<?> loaded = ClasspathUtil.getSystemClass(normalize(name));
			return new ClassReader(loaded.getName());
		} catch(ClassNotFoundException | IOException e) {
			// Expected / allowed: ignore these
		} catch(Exception ex) {
			// Unexpected
			throw new IllegalStateException("Failed to load class from runtime: " + name, ex);
		}
		return null;
	}


	/**
	 * @param internal
	 * 		Internal class name.
	 *
	 * @return Standard class name.
	 */
	private static String normalize(String internal) {
		return internal.replace("/", ".").replace("$", ".");
	}
}
