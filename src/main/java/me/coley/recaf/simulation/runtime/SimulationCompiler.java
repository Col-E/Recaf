package me.coley.recaf.simulation.runtime;

import org.objectweb.asm.tree.ClassNode;

import java.util.HashMap;
import java.util.Map;

public final class SimulationCompiler {
	private final Object compileLock = new Object();
	private final Map<String, ClassNode> compiled = new HashMap<>();

}
