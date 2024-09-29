package software.coley.recaf.ui.config;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableCollection;
import software.coley.observables.ObservableInteger;
import software.coley.observables.ObservableObject;
import software.coley.observables.ObservableString;
import software.coley.recaf.config.BasicCollectionConfigValue;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.properties.builtin.InputFilePathProperty;
import software.coley.recaf.services.phantom.GeneratedPhantomWorkspaceResource;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceDirectoryResource;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Config for tracking recent file interactions.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class RecentFilesConfig extends BasicConfigContainer {
	public static final String ID = "recent-workspaces";
	private final ObservableInteger maxRecentWorkspaces = new ObservableInteger(10);
	private final ObservableCollection<WorkspaceModel, List<WorkspaceModel>> recentWorkspaces = new ObservableCollection<>(ArrayList::new);
	private final ObservableString lastWorkspaceOpenDirectory = new ObservableString(System.getProperty("user.dir"));
	private final ObservableString lastWorkspaceExportDirectory = new ObservableString(System.getProperty("user.dir"));
	private final ObservableString lastClassExportDirectory = new ObservableString(System.getProperty("user.dir"));

	@Inject
	public RecentFilesConfig() {
		super(ConfigGroups.SERVICE_IO, ID + CONFIG_SUFFIX);
		// Add values
		addValue(new BasicConfigValue<>("max-recent-workspaces", int.class, maxRecentWorkspaces));
		addValue(new BasicCollectionConfigValue<>("recent-workspaces", List.class, WorkspaceModel.class, recentWorkspaces, true));
		addValue(new BasicConfigValue<>("last-workspace-open-path", String.class, lastWorkspaceOpenDirectory));
		addValue(new BasicConfigValue<>("last-workspace-export-path", String.class, lastWorkspaceExportDirectory));
		addValue(new BasicConfigValue<>("last-class-export-path", String.class, lastClassExportDirectory));
	}

	/**
	 * @param workspace
	 * 		Workspace to add to {@link #getRecentWorkspaces()}.
	 */
	public void addWorkspace(@Nonnull Workspace workspace) {
		// Only allow serializable workspaces
		WorkspaceResource primaryResource = workspace.getPrimaryResource();
		if (!ResourceModel.isSupported(primaryResource))
			return;

		// Wrap to model
		ResourceModel primary = ResourceModel.from(primaryResource);
		List<ResourceModel> libraries = workspace.getSupportingResources().stream()
				.map(ResourceModel::from)
				.filter(Objects::nonNull)
				.toList();
		WorkspaceModel workspaceModel = new WorkspaceModel(primary, libraries);

		// Update recent workspace list, where new items are inserted at the beginning.
		// Old items are removed near the end.
		List<WorkspaceModel> updatedList = new ArrayList<>(recentWorkspaces.getValue());
		if (recentWorkspaces.contains(workspaceModel))
			updatedList.remove(workspaceModel);
		while (updatedList.size() >= maxRecentWorkspaces.getValue())
			updatedList.remove(updatedList.size() - 1);
		updatedList.add(0, workspaceModel);
		recentWorkspaces.setValue(updatedList);
	}

	/**
	 * Refresh available workspaces, removing any items that cannot be loaded from the list.
	 *
	 * @see WorkspaceModel#canLoadWorkspace()
	 */
	public void clearUnloadable() {
		List<WorkspaceModel> current = recentWorkspaces.getValue();
		List<WorkspaceModel> loadable = current.stream()
				.filter(WorkspaceModel::canLoadWorkspace)
				.toList();
		if (current.size() != loadable.size())
			recentWorkspaces.setValue(loadable);
	}

	/**
	 * @return Number of recent items to track.
	 */
	@Nonnull
	public ObservableInteger getMaxRecentWorkspaces() {
		return maxRecentWorkspaces;
	}

	/**
	 * @return Recent workspaces.
	 */
	@Nonnull
	public ObservableObject<List<WorkspaceModel>> getRecentWorkspaces() {
		return recentWorkspaces;
	}

	/**
	 * @return Last path used to open a workspace with.
	 */
	@Nonnull
	public ObservableString getLastWorkspaceOpenDirectory() {
		return lastWorkspaceOpenDirectory;
	}

	/**
	 * @return Last path used to export a workspace to.
	 */
	@Nonnull
	public ObservableString getLastWorkspaceExportDirectory() {
		return lastWorkspaceExportDirectory;
	}

	/**
	 * @return Last path used to export a class to.
	 */
	@Nonnull
	public ObservableString getLastClassExportDirectory() {
		return lastClassExportDirectory;
	}

	/**
	 * Basic wrapper for workspaces.
	 *
	 * @param primary
	 * 		Primary resource of the workspace.
	 * @param libraries
	 * 		Workspace supporting libraries.
	 *
	 * @author Matt Coley
	 * @see ResourceModel
	 */
	public record WorkspaceModel(ResourceModel primary, List<ResourceModel> libraries) {
		/**
		 * @return {@code true} when the files still exist at their expected locations.
		 */
		public boolean canLoadWorkspace() {
			Path path = Paths.get(primary().path());
			if (!Files.exists(path))
				return false;
			for (ResourceModel model : libraries()) {
				path = Paths.get(model.path());
				if (!Files.exists(path))
					return false;
			}
			return true;
		}
	}

	/**
	 * Wrapper for a resources content source path.
	 *
	 * @param path
	 * 		Path to the resource content source.
	 *
	 * @author Matt Coley
	 */
	public record ResourceModel(String path) {
		/**
		 * @param resource
		 * 		Some resource sourced from a file or directory.
		 *
		 * @return Representation of the content source.
		 */
		@Nullable
		public static ResourceModel from(@Nonnull WorkspaceResource resource) {
			if (resource instanceof WorkspaceFileResource fileResource) {
				FileInfo fileInfo = fileResource.getFileInfo();
				Path inputPath = InputFilePathProperty.get(fileInfo);
				if (inputPath != null)
					return new ResourceModel(StringUtil.pathToAbsoluteString(inputPath));
				return new ResourceModel(fileInfo.getName());
			} else if (resource instanceof WorkspaceDirectoryResource fileResource) {
				return new ResourceModel(StringUtil.pathToAbsoluteString(fileResource.getDirectoryPath()));
			} else if (resource instanceof GeneratedPhantomWorkspaceResource) {
				// Intentionally left out
				return null;
			}
			throw new UnsupportedOperationException("Cannot serialize content source of type: " +
					resource.getClass().getName());
		}

		/**
		 * @param resource
		 * 		Some resource.
		 *
		 * @return {@code true} when it can be represented by this model.
		 */
		public static boolean isSupported(@Nonnull WorkspaceResource resource) {
			if (resource instanceof WorkspaceFileResource fileResource)
				return InputFilePathProperty.get(fileResource.getFileInfo()) != null;
			return resource instanceof WorkspaceDirectoryResource;
		}

		/**
		 * @return Shortened path of resource's content source.
		 */
		@Nonnull
		public String getSimpleName() {
			String name = path;
			int slashIndex = name.lastIndexOf('/');
			if (slashIndex > 0)
				name = name.substring(slashIndex + 1);
			return name;
		}

		/**
		 * @return Path to the resource content source. Can be a file path, maven coordinates, or url.
		 */
		@Override
		public String path() {
			return path;
		}
	}
}
