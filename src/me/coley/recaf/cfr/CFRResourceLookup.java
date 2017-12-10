package me.coley.recaf.cfr;

import java.util.Map;

import org.objectweb.asm.tree.ClassNode;

import me.coley.recaf.Recaf;
import me.coley.recaf.asm.Asm;

/**
 * Lookup helper for CFR since it requests extra data <i>(Other classes)</i> for
 * more accurate decompilation.
 */
public class CFRResourceLookup {
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
				bytes = Asm.toBytes(override);
			} else {
				Map<String, ClassNode> classes = Recaf.INSTANCE.jarData.classes;
				if (classes.containsKey(path)) {
					bytes = Asm.toBytes(classes.get(path));
				} else {
					ClassNode runtime = Asm.getNode(Class.forName(path.replace("/", ".")));
					if (runtime != null) {
						bytes = Asm.toBytes(runtime);
					}
				}
			}
		} catch (ClassNotFoundException e) {
			// Do nothing
		} catch (Exception e) {
			Recaf.INSTANCE.ui.openException(e);
		}
		return bytes;
	}

}