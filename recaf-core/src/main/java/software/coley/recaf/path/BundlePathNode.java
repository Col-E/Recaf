package software.coley.recaf.path;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import net.greypanther.natsort.CaseInsensitiveSimpleNaturalComparator;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Map;
import java.util.Set;

/**
 * Path node for {@link Bundle} types.
 *
 * @author Matt Coley
 */
@SuppressWarnings("rawtypes")
public class BundlePathNode extends AbstractPathNode<WorkspaceResource, Bundle> {
	/**
	 * Type identifier for bundle nodes.
	 */
	public static final String TYPE_ID = "bundle";

	/**
	 * Node without parent.
	 *
	 * @param bundle
	 * 		Bundle value.
	 */
	public BundlePathNode(@Nonnull Bundle<?> bundle) {
		this(null, bundle);
	}

	/**
	 * Node with parent.
	 *
	 * @param parent
	 * 		Parent node.
	 * @param bundle
	 * 		Bundle value.
	 *
	 * @see ResourcePathNode#child(Bundle)
	 */
	public BundlePathNode(@Nullable ResourcePathNode parent, @Nonnull Bundle<?> bundle) {
		super(TYPE_ID, parent, bundle);
	}

	/**
	 * @param directory
	 * 		Directory to wrap in path node.
	 *
	 * @return Path node of directory, with the current bundle as parent.
	 */
	@Nonnull
	public DirectoryPathNode child(@Nullable String directory) {
		return new DirectoryPathNode(this, directory == null ? "" : directory);
	}

	/**
	 * @return {@code true} when the path is in the resource's immediate JVM class bundle.
	 */
	public boolean isInJvmBundle() {
		WorkspaceResource resource = parentValue();
		return resource != null && resource.getJvmClassBundle() == getValue();
	}

	/**
	 * @return {@code true} when the path is in one of the resource's Android bundles.
	 */
	@SuppressWarnings("all")
	public boolean isInAndroidBundle() {
		WorkspaceResource resource = parentValue();
		return resource != null && resource.getAndroidClassBundles().containsValue(getValue());
	}

	/**
	 * @return {@code true} when the path is in the resource's immediate file bundle.
	 */
	public boolean isInFileBundle() {
		WorkspaceResource resource = parentValue();
		return resource != null && resource.getFileBundle() == getValue();
	}

	/**
	 * @return {@code true} when the path is in one of the resource's versioned class bundles.
	 */
	public boolean isInVersionedJvmBundle() {
		WorkspaceResource resource = parentValue();
		if (resource != null && getValue() instanceof JvmClassBundle jvmBundle)
			return resource.getVersionedJvmClassBundles().containsValue(jvmBundle);
		return false;
	}

	/**
	 * @return Bit-mask used for ordering in {@link #compareTo(PathNode)}.
	 */
	private int bundleMask() {
		return ((isInJvmBundle() ? 1 : 0) << 16) |
				((isInVersionedJvmBundle() ? 1 : 0) << 14) |
				((isInAndroidBundle() ? 1 : 0) << 12) |
				((isInFileBundle() ? 1 : 0) << 10);
	}

	@Override
	public ResourcePathNode getParent() {
		return (ResourcePathNode) super.getParent();
	}

	@Nonnull
	@Override
	public Set<String> directParentTypeIds() {
		return Set.of(ResourcePathNode.TYPE_ID);
	}

	@Override
	public int localCompare(PathNode<?> o) {
		if (this == o) return 0;

		if (o instanceof BundlePathNode bundlePathNode) {
			int cmp = -Integer.compare(bundleMask(), bundlePathNode.bundleMask());

			// Order dex class bundles to be in alphabetical order.
			Bundle bundle = getValue();
			Object otherBundle = o.getValue();
			if (cmp == 0 && getParent() != null &&
					bundle instanceof AndroidClassBundle &&
					otherBundle instanceof AndroidClassBundle) {
				WorkspaceResource resource = getParent().getValue();
				Set<Map.Entry<String, AndroidClassBundle>> androidBundles = resource.getAndroidClassBundles().entrySet();
				String dexName = androidBundles.stream()
						.filter(e -> e.getValue() == bundle)
						.map(Map.Entry::getKey)
						.findFirst()
						.orElse(null);
				String otherDexName = androidBundles.stream()
						.filter(e -> e.getValue() == otherBundle)
						.map(Map.Entry::getKey)
						.findFirst()
						.orElse(null);
				return CaseInsensitiveSimpleNaturalComparator.getInstance().compare(dexName, otherDexName);
			}
		}
		return 0;
	}
}
