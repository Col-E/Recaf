package software.coley.recaf.workspace.model.bundle;

/**
 * Basic versioned JVM class bundle implementation.
 *
 * @author Matt Coley
 */
public class BasicVersionedClassBundle extends BasicJvmClassBundle implements VersionedClassBundle {
	private final int version;

	/**
	 * @param version
	 * 		Associated version.
	 */
	public BasicVersionedClassBundle(int version) {
		this.version = version;
	}

	@Override
	public int version() {
		return version;
	}
}
