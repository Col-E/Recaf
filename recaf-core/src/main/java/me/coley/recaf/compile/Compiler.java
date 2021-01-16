package me.coley.recaf.compile;

import me.coley.recaf.plugin.tools.Tool;
import me.coley.recaf.plugin.tools.ToolOption;

/**
 * Compiler wrapper.
 *
 * @author Matt Coley
 */
public abstract class Compiler extends Tool<ToolOption<?>> {
	protected Compiler(String name, String version) {
		super(name, version);
	}

	// TODO: What should we pass to the compiler? Just the string of the source?
	//   - Would there be any way to supply "hints" to certain compilers if we
	//     give it more?
}
