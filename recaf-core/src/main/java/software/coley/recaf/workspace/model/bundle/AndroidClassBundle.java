package software.coley.recaf.workspace.model.bundle;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Bundle of Android classes in a {@link WorkspaceResource}.
 *
 * @author Matt Coley
 */
public interface AndroidClassBundle extends ClassBundle<AndroidClassInfo> {
	/**
	 * The DEX file format version.
	 *
	 * @return Container version.
	 */
	int getVersion();

	/**
	 * According to <a href="https://source.android.com/docs/core/runtime/dex-format">Dalvik executable format</a>
	 * this content is:
	 * <blockquote>
	 * data used in statically linked files. The format of the data in this section is left unspecified by this document.
	 * This section is empty in unlinked files, and runtime implementations may use it as they see fit.
	 * </blockquote>
	 *
	 * @return DEX link data.
	 */
	byte[] getLinkData();

	/**
	 * @return Metadata associated with this DEX entry.
	 */
	@Nonnull
	AndroidDexMetadata getDexMetadata();

	/**
	 * Discard hidden-API data.
	 * <p>
	 * Should be used after adding/removing classes to the bundle,
	 * or when the members of a class have been re-ordered <i>(or added/removed)</i>.
	 */
	void discardHiddenApiData();
}
