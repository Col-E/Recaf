package me.coley.recaf.plugin.tools;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * Manager of implementations of a {@link Tool} type.
 *
 * @param <T>
 * 		Tool implementation type.
 *
 * @author Matt Coley
 */
public class ToolManager<T extends Tool<?>> {
	private final Map<String, T> toolMap = new TreeMap<>();

	/**
	 * Add a tool to the manager.
	 *
	 * @param tool
	 * 		Tool implementation.
	 */
	public void register(T tool) {
		this.toolMap.put(tool.getName(), tool);
	}

	/**
	 * @param name
	 * 		Name of the tool, see {@link Tool#getName()}.
	 *
	 * @return Instance of decompiler.
	 */
	public T get(String name) {
		return toolMap.get(name);
	}

	/**
	 * @return Collection of all registered tools.
	 */
	public Collection<T> getRegisteredImpls() {
		return toolMap.values();
	}
}
