package software.coley.recaf.workspace.model.bundle;

import software.coley.recaf.info.AndroidClassInfo;

/**
 * Basic Android class bundle implementation.
 *
 * @author Matt Coley
 */
public class BasicAndroidClassBundle extends BasicBundle<AndroidClassInfo> implements AndroidClassBundle {
	private final int version;
	private final byte[] link;

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
		super();
		this.version = version;
		this.link = link;
	}

	@Override
	public int getVersion() {
		return version;
	}

	@Override
	public byte[] getLinkData() {
		return link;
	}
}
