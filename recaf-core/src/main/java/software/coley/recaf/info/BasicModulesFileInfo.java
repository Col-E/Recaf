package software.coley.recaf.info;

import software.coley.recaf.info.builder.ModulesFileInfoBuilder;

/**
 * Basic implementation of Modules file info.
 *
 * @author Matt Coley
 */
public class BasicModulesFileInfo extends BasicFileInfo implements ModulesFileInfo {
	/**
	 * @param builder
	 * 		Builder to pull information from.
	 */
	public BasicModulesFileInfo(ModulesFileInfoBuilder builder) {
		super(builder);
	}
}
