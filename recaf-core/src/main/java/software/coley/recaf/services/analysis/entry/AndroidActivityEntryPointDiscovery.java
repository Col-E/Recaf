package software.coley.recaf.services.analysis.entry;

import com.google.devrel.gmscore.tools.apk.arsc.XmlAttribute;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.util.android.AndroidXmlUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.List;

/**
 * Discovery for Android manifest activities.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class AndroidActivityEntryPointDiscovery implements EntryPointDiscovery {
	@Nonnull
	@Override
	public EntryPointKind kind() {
		return EntryPointKind.ANDROID_ACTIVITY;
	}

	@Nonnull
	@Override
	public List<EntryPoint> findEntryPoints(@Nonnull Workspace workspace, @Nonnull WorkspaceResource resource) {
		List<EntryPoint> entries = new ArrayList<>();
		for (AndroidXmlUtil.XmlElementData element : AndroidXmlUtil.getManifestStartElements(workspace, resource)) {
			if (!"activity".equals(element.element().getName()))
				continue;

			// We have an activity, we need to extract the associated class name from it.
			for (XmlAttribute attribute : element.element().getAttributes()) {
				String attributeName = AndroidXmlUtil.getString(element.strings(), attribute.nameIndex());
				if (!"name".equals(attributeName))
					continue;
				String activityName = AndroidXmlUtil.getString(element.strings(), attribute.rawValueIndex());
				if (activityName == null)
					continue;
				ClassPathNode activityPath = workspace.findAndroidClass(activityName.replace('.', '/'));
				if (activityPath != null)
					entries.add(new EntryPoint(kind(), activityPath, null));
			}
		}
		return entries;
	}
}
