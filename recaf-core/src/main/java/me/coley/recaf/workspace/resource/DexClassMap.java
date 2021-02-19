package me.coley.recaf.workspace.resource;

import org.jf.dexlib2.dexbacked.DexBackedDexFile;

import java.util.HashMap;

/**
 * Map of Android dex classes in the resource.
 *
 * @author Matt Coley
 */
public class DexClassMap extends ResourceItemMap<DexClassInfo> {
	private final DexBackedDexFile dexFile;

	/**
	 * @param container
	 * 		Parent resource.
	 * @param dexFile
	 * 		Backing dex file.
	 */
	public DexClassMap(Resource container, DexBackedDexFile dexFile) {
		super(container, new HashMap<>());
		this.dexFile = dexFile;
	}

	/**
	 * @return Backing dex file.
	 */
	public DexBackedDexFile getDexFile() {
		return dexFile;
	}
}
