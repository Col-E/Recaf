package software.coley.recaf.services.phantom;

import jakarta.annotation.Nonnull;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.bundle.VersionedJvmClassBundle;
import software.coley.recaf.workspace.model.resource.BasicWorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResourceBuilder;

import java.util.Map;
import java.util.NavigableMap;

/**
 * A special sub-type of a workspace resource indicating it originates from {@link PhantomGenerator}.
 *
 * @author Matt Coley
 */
public class GeneratedPhantomWorkspaceResource extends BasicWorkspaceResource {
	/**
	 * @param builder
	 * 		Builder to pull information from.
	 */
	public GeneratedPhantomWorkspaceResource(@Nonnull WorkspaceResourceBuilder builder) {
		super(builder);
	}

	/**
	 * @param jvmClassBundle
	 * 		Immediate classes.
	 * @param fileBundle
	 * 		Immediate files.
	 * @param versionedJvmClassBundles
	 * 		Version specific classes.
	 * @param androidClassBundles
	 * 		Android bundles.
	 * @param embeddedResources
	 * 		Embedded resources <i>(like JAR in JAR)</i>
	 * @param containingResource
	 * 		Parent resource <i>(If we are the JAR within a JAR)</i>.
	 */
	public GeneratedPhantomWorkspaceResource(JvmClassBundle jvmClassBundle,
											 FileBundle fileBundle,
											 NavigableMap<Integer, VersionedJvmClassBundle> versionedJvmClassBundles,
											 Map<String, AndroidClassBundle> androidClassBundles,
											 Map<String, WorkspaceFileResource> embeddedResources,
											 WorkspaceResource containingResource) {
		super(jvmClassBundle, fileBundle, versionedJvmClassBundles, androidClassBundles, embeddedResources, containingResource);
	}
}
