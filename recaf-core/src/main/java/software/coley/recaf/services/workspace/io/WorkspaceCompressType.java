package software.coley.recaf.services.workspace.io;

import software.coley.recaf.info.Info;
import software.coley.recaf.info.properties.builtin.ZipCompressionProperty;

/**
 * Compression option for ZIP/JAR outputs in {@link WorkspaceExportOptions}.
 *
 * @author Matt Coley
 */
public enum WorkspaceCompressType {
	/**
	 * Match the original compression of a {@link Info} item by checking {@link ZipCompressionProperty}.
	 * When unknown, defaults to enabling compression.
	 */
	MATCH_ORIGINAL,
	/**
	 * Compress items only when if it will yield more compact data.
	 * Some smaller files do not compress well due to the overhead cost of the compression.
	 */
	SMART,
	/**
	 * Compress all items in the output.
	 */
	ALWAYS,
	/**
	 * Do not compress any items in the output.
	 */
	NEVER,
}
