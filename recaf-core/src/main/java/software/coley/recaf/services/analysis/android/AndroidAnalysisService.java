package software.coley.recaf.services.analysis.android;

import com.google.devrel.gmscore.tools.apk.arsc.XmlAttribute;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.services.Service;
import software.coley.recaf.util.android.AndroidXmlUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Service for Android-specific analysis.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class AndroidAnalysisService implements Service {
	public static final String SERVICE_ID = "android-analysis";
	private final AndroidAnalysisConfig config;

	@Inject
	public AndroidAnalysisService(@Nonnull AndroidAnalysisConfig config) {
		this.config = config;
	}

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Resource to inspect, including embedded resources.
	 *
	 * @return Requested Android permissions in traversal order.
	 */
	@Nonnull
	public List<AndroidPermissionEntry> findRequestedPermissions(@Nonnull Workspace workspace,
	                                                             @Nonnull WorkspaceResource resource) {
		List<AndroidPermissionEntry> permissions = new ArrayList<>();

		Queue<WorkspaceResource> resourceQueue = new ArrayDeque<>();
		resourceQueue.add(resource);
		while (!resourceQueue.isEmpty()) {
			WorkspaceResource currentResource = resourceQueue.remove();

			// Walk through all chunks and their children to find permission attributes.
			for (AndroidXmlUtil.XmlElementData element : AndroidXmlUtil.getManifestStartElements(workspace, currentResource)) {
				// If the chunk is a permission chunk then we want to look for the "name" attribute.
				// Otherwise, we look for the "permission" attribute.
				String elementName = element.element().getName();
				boolean isPermissionElement = "uses-permission".equals(elementName) || "permission".equals(elementName);
				for (XmlAttribute attribute : element.element().getAttributes()) {
					String attributeName = AndroidXmlUtil.getString(element.strings(), attribute.nameIndex());
					String expectedAttributeName = isPermissionElement ? "name" : "permission";
					if (!expectedAttributeName.equals(attributeName))
						continue;
					String permission = AndroidXmlUtil.getString(element.strings(), attribute.rawValueIndex());
					if (permission != null) {
						permissions.add(new AndroidPermissionEntry(
								element.filePath(),
								permission,
								elementName,
								attributeName
						));
					}
				}
			}
			resourceQueue.addAll(currentResource.getEmbeddedResources().values());
		}
		return permissions;
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public AndroidAnalysisConfig getServiceConfig() {
		return config;
	}
}
