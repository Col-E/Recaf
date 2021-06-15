package me.coley.recaf.code.parse;

import javassist.ClassPath;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.workspace.Workspace;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;

/**
 * Javassist classpath implementation that pulls classes from a workspace.
 *
 * @author Matt Coley
 */
public class WorkspaceClassPath implements ClassPath {
	private final Workspace workspace;

	/**
	 * @param workspace
	 * 		Workspace to pull classes from.
	 */
	public WorkspaceClassPath(Workspace workspace) {
		this.workspace = workspace;
	}

	@Override
	public InputStream openClassfile(String classname) {
		ClassInfo info = workspace.getResources().getClass(classname);
		if (info != null)
			return new ByteArrayInputStream(info.getValue());
		return null;
	}

	@Override
	public URL find(String classname) {
		// Not needed for this use case.
		return null;
	}
}
