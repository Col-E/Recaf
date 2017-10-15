package me.coley.recaf.cfr;

import java.util.Map;

import org.objectweb.asm.tree.ClassNode;

import me.coley.recaf.Recaf;

/**
 * Lookup helper for CFR since it requests extra data <i>(Other classes)</i> for more accurate decompilation. 
 */
public class CFRResourceLookup {
	private final Recaf program = Recaf.INSTANCE;
	private final ClassNode override;
	
	public CFRResourceLookup() {
		this(null);
	}

	public CFRResourceLookup(ClassNode override) {
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
			program.gui.displayError(e);
		}
		return bytes;
	}

}
