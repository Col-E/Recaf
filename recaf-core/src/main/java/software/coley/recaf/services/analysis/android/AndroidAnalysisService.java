package software.coley.recaf.services.analysis.android;

import com.google.devrel.gmscore.tools.apk.arsc.XmlAttribute;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.services.Service;
import software.coley.recaf.util.ResourceUtil;
import software.coley.recaf.util.android.AndroidXmlUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Service for Android-specific analysis.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class AndroidAnalysisService implements Service {
	public static final String SERVICE_ID = "android-analysis";

	private static final String PERMISSION_PREFIX = "android.permission.";
	private static final List<AndroidPermissionLevel> UNKNOWN_PERMISSION_LEVELS = List.of(new AndroidPermissionLevel("unknown", "unknown"));
	private static final Map<String, List<String>> PERMISSION_LEVELS;
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
	 * @return Requested Android permissions and their resolved protection levels in traversal order.
	 */
	@Nonnull
	public List<AndroidPermissionDetails> findRequestedPermissionDetails(@Nonnull Workspace workspace,
	                                                                     @Nonnull WorkspaceResource resource) {
		List<AndroidPermissionEntry> permissions = findRequestedPermissions(workspace, resource);
		List<AndroidPermissionDetails> details = new ArrayList<>(permissions.size());
		for (AndroidPermissionEntry permission : permissions)
			details.add(new AndroidPermissionDetails(permission, resolvePermissionLevels(permission.permission())));
		return details;
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
	private List<AndroidPermissionLevel> resolvePermissionLevels(@Nonnull String permissionName) {
		// We map short permission names to their levels,
		// so we need to strip the "android.permission" prefix from the permission name.
		// If it is not a core Android permission then we won't have any information on it, so we return the unknown level.
		String shortPermissionName = normalizePermissionName(permissionName);
		if (shortPermissionName == null)
			return UNKNOWN_PERMISSION_LEVELS;

		// Check if we have any levels for this permission.
		List<String> rawLevels = PERMISSION_LEVELS.get(shortPermissionName);
		if (rawLevels == null || rawLevels.isEmpty())
			return UNKNOWN_PERMISSION_LEVELS;

		// Map the raw levels "foo|bar" to their base level "foo" and full level "foo|bar".
		List<AndroidPermissionLevel> resolvedLevels = new ArrayList<>(rawLevels.size());
		for (String rawLevel : rawLevels) {
			AndroidPermissionLevel permissionLevel = parsePermissionLevel(rawLevel);
			if (permissionLevel == null)
				return UNKNOWN_PERMISSION_LEVELS;
			resolvedLevels.add(permissionLevel);
		}
		return List.copyOf(resolvedLevels);
	}

	@Nonnull
	private static Map<String, List<String>> loadPermissionLevels() {
		InputStream stream = ResourceUtil.resource("android/permission-levels.csv");
		if (stream == null)
			return Map.of();

		Map<String, Set<String>> permissionLevels = new TreeMap<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split(",", 2);
				if (parts.length != 2)
					continue;

				String permissionName = parts[0].trim();
				String rawLevel = parts[1].trim().toLowerCase();
				if (permissionName.isEmpty() || rawLevel.isEmpty())
					continue;

				permissionLevels.computeIfAbsent(permissionName, ignored -> new TreeSet<>()).add(rawLevel);
			}
		} catch (IOException ex) {
			return Map.of();
		}

		Map<String, List<String>> resolvedPermissionLevels = new TreeMap<>();
		for (Map.Entry<String, Set<String>> entry : permissionLevels.entrySet())
			resolvedPermissionLevels.put(entry.getKey(), List.copyOf(entry.getValue()));
		return Map.copyOf(resolvedPermissionLevels);
	}

	@Nullable
	private static AndroidPermissionLevel parsePermissionLevel(@Nonnull String rawLevel) {
		String normalizedRawLevel = rawLevel.trim().toLowerCase();
		if (normalizedRawLevel.isEmpty())
			return null;

		int separator = normalizedRawLevel.indexOf('|');
		String baseLevel = separator >= 0 ? normalizedRawLevel.substring(0, separator) : normalizedRawLevel;
		if (baseLevel.isEmpty())
			return null;

		return new AndroidPermissionLevel(baseLevel, normalizedRawLevel);
	}

	@Nullable
	private static String normalizePermissionName(@Nonnull String permissionName) {
		return permissionName.startsWith(PERMISSION_PREFIX) ?
				permissionName.substring(PERMISSION_PREFIX.length()) :
				null;
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

	static {
		PERMISSION_LEVELS = loadPermissionLevels();
	}
}
