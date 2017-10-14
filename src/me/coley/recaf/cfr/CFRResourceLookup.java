package me.coley.recaf.cfr;

import java.util.Map;

import org.objectweb.asm.tree.ClassNode;

import me.coley.recaf.Recaf;

/**
 * Lookup helper for CFR since it requests this data in order to show anonymous
 * inner classes and such.
 */
public class CFRResourceLookup {
	private final Recaf program;
	private final ClassNode override;

	public CFRResourceLookup(Recaf program) {
		this(program, null);
	}

	public CFRResourceLookup(Recaf program, ClassNode override) {
		this.program = program;
		this.override = override;
	}

	public byte[] get(String path) {
		byte[] bytes = null;
		try {
			if (override != null && path.equals(override.name)) {
				bytes = program.asm.toBytes(override);
			} else {
				Map<String, ClassNode> classes = program.jarData.classes;
				if (classes.containsKey(path)) {
					bytes = program.asm.toBytes(classes.get(path));
				} else {
					ClassNode runtime = program.asm.getNode(Class.forName(path.replace("/", ".")));
					if (runtime != null) {
						bytes = program.asm.toBytes(runtime);
					}
				}
			}
		} catch (Exception e) {
			program.window.displayError(e);
		}
		return bytes;
	}

}
