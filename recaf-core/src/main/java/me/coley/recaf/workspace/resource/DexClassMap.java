package me.coley.recaf.workspace.resource;

import me.coley.recaf.code.DexClassInfo;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Map of Android dex classes in the resource.
 *
 * @author Matt Coley
 */
public class DexClassMap extends ResourceItemMap<DexClassInfo> implements DexFile {
	public Opcodes opcodes;

	/**
	 * @param container
	 * 		Parent resource.
	 * @param opcodes
	 * 		Opcode set used for the current dex.
	 */
	public DexClassMap(Resource container, Opcodes opcodes) {
		super(container, new HashMap<>());
		this.opcodes = opcodes;
	}

	@Override
	public Set<? extends ClassDef> getClasses() {
		Set<ClassDef> defs = new HashSet<>();
		for (DexClassInfo info : values()) {
			defs.add(info.getClassDef());
		}
		return defs;
	}

	/**
	 * @return Opcode set used for the current dex.
	 */
	public Opcodes getOpcodes() {
		return opcodes;
	}

	/**
	 * @param opcodes
	 * 		New opcode set to use for the current dex.
	 */
	public void setOpcodes(Opcodes opcodes) {
		this.opcodes = opcodes;
	}
}
