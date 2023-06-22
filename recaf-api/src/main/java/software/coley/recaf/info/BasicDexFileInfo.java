package software.coley.recaf.info;

import software.coley.recaf.info.builder.DexFileInfoBuilder;

/**
 * Basic implementation of an Android DEX file info.
 *
 * @author Matt Coley
 */
public class BasicDexFileInfo extends BasicFileInfo implements DexFileInfo {
	/**
	 * @param builder
	 * 		Builder to pull information from.
	 */
	public BasicDexFileInfo(DexFileInfoBuilder builder) {
		super(builder);
	}
}
