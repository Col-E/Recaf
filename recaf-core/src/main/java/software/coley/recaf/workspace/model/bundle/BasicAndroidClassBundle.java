package software.coley.recaf.workspace.model.bundle;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.AndroidClassInfo;

/**
 * Basic Android class bundle implementation.
 *
 * @author Matt Coley
 */
public class BasicAndroidClassBundle extends BasicBundle<AndroidClassInfo> implements AndroidClassBundle {
	private final int version;
	private final byte[] link;
	private AndroidDexMetadata metadata;

	/**
	 * Default constructor with no data. Should not be used outside of tests.
	 */
	public BasicAndroidClassBundle() {
		this(0, new byte[0]);
	}

	/**
	 * @param version
	 * 		Dex container version.
	 * @param link
	 * 		Dex container link data.
	 */
	public BasicAndroidClassBundle(int version, byte[] link) {
		this(version, link, AndroidDexMetadata.empty());
	}

	/**
	 * @param version
	 * 		Dex container version.
	 * @param link
	 * 		Dex container link data.
	 * @param metadata
	 * 		Metadata associated with the DEX entry.
	 */
	public BasicAndroidClassBundle(int version, byte[] link, @Nonnull AndroidDexMetadata metadata) {
		super();
		this.version = version;
		this.link = link;
		this.metadata = metadata;
	}

	@Override
	public int getVersion() {
		return version;
	}

	@Override
	public byte[] getLinkData() {
		return link;
	}

	@Override
	@Nonnull
	public AndroidDexMetadata getDexMetadata() {
		return metadata;
	}

	@Override
	public void discardHiddenApiData() {
		metadata = metadata.withoutHiddenApiData();
	}
}
