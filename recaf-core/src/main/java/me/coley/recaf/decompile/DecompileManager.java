package me.coley.recaf.decompile;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * Manager of decompilers.
 *
 * @author Matt Coley
 */
// TODO: Supply this with @Inject or put it in Controller?
//   - If @Inject, must integrate a DI system, but then any user just needs to ask for the types they depend on.
//   - If put in Controller, simple, but similar services also would be put there. Can get bloated if repeated often.
public class DecompileManager {
	private final Map<String, Decompiler> decompilerMap = new TreeMap<>();

	/**
	 * Add a decompiler to the manager.
	 *
	 * @param decompiler
	 * 		Decompiler implementation.
	 */
	public void register(Decompiler decompiler) {
		this.decompilerMap.put(decompiler.getName(), decompiler);
	}

	/**
	 * @param name
	 * 		Name of the decompiler, see {@link Decompiler#getName()}.
	 *
	 * @return Instance of decompiler.
	 */
	public Decompiler get(String name) {
		return decompilerMap.get(name);
	}

	/**
	 * @return Collection of all registered decompilers.
	 */
	public Collection<Decompiler> getRegisteredDecompilers() {
		return decompilerMap.values();
	}
}
