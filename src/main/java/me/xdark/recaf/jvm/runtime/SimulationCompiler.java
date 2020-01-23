package me.xdark.recaf.jvm.runtime;

import me.coley.recaf.workspace.Workspace;

import java.util.HashMap;
import java.util.Map;

public final class SimulationCompiler {
	private final Object compileLock = new Object();
	private final Map<String, VirtualClass> compiled = new HashMap<>();
	private final Workspace workspace;

	public SimulationCompiler(Workspace workspace) {
		this.workspace = workspace;
	}

	public VirtualClass compileClass(String className) {
		VirtualClass compiled = this.compiled.get(className);
		if (compiled != null) return compiled;
		throw new UnsupportedOperationException();
	}
}
