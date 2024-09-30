package software.coley.recaf.workspace.model.bundle;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.properties.builtin.VersionedClassProperty;

/**
 * Basic versioned JVM class bundle implementation.
 *
 * @author Matt Coley
 */
public class BasicVersionedJvmClassBundle extends BasicJvmClassBundle implements VersionedJvmClassBundle, BundleListener<JvmClassInfo> {
	private final int version;

	/**
	 * @param version
	 * 		Associated version.
	 */
	public BasicVersionedJvmClassBundle(int version) {
		this.version = version;

		// Register self as listener. We'll apply the versioned class property to items added to this bundle.
		addBundleListener(this);
	}

	@Override
	public int version() {
		return version;
	}

	@Override
	public void onNewItem(@Nonnull String key, @Nonnull JvmClassInfo value) {
		VersionedClassProperty.set(value, version);
	}

	@Override
	public void onUpdateItem(@Nonnull String key, @Nonnull JvmClassInfo oldValue, @Nonnull JvmClassInfo newValue) {
		VersionedClassProperty.set(newValue, version);
	}

	@Override
	public void onRemoveItem(@Nonnull String key, @Nonnull JvmClassInfo value) {
		// no-op
	}
}
