package me.coley.recaf.workspace;

import com.eclipsesource.json.*;

import java.io.File;
import java.util.List;

public class Workspace {
	/**
	 * Primary file being worked on.
	 */
	private JavaResource primary;
	/**
	 * Libraries of the primary file. Useful for additional analysis capabilities.
	 */
	private List<JavaResource> libraries;

	public Workspace(JavaResource primary, List<JavaResource> libraries) {
		this.primary = primary;
		this.libraries = libraries;
	}

	/**
	 * @return Primary file being worked on.
	 */
	public JavaResource getPrimary() {
		return primary;
	}

	/**
	 * @return Libraries of the {@link #getPrimary() primary file}.
	 */
	public List<JavaResource> getLibraries() {
		return libraries;
	}
}