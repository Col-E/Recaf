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
		WorkspaceModel source = workspaceModelFor(workspace);
		if (source == null)
			return;

		// Update the list of recent workspaces, removing any existing entry for the same workspace.
		List<WorkspaceModel> updatedList = new ArrayList<>(recentWorkspaces.getValue());
		String exportPath = null;
		for (int i = 0; i < updatedList.size(); i++) {
			WorkspaceModel existing = updatedList.get(i);
			if (existing.equalModels(source)) {
				// This model represents the same workspace, so preserve the export path if it exists.
				exportPath = existing.exportPath();
				updatedList.remove(i);
				break;
			}
		}
		if (maxRecentWorkspaces.getValue() <= 0)
			return;

		// Prune the list to make room for the new workspace.
		while (updatedList.size() >= maxRecentWorkspaces.getValue())
			updatedList.removeLast();

		// Add the workspace, preserving the export path if it was found in a prior entry.
		updatedList.addFirst(new WorkspaceModel(source.primary(), source.libraries(), exportPath));
		recentWorkspaces.setValue(updatedList);
	}

	/**
	 * Record the successful export path for a workspace.
	 *
	 * @param workspace
	 * 		Workspace exported.
	 * @param path
	 * 		Successful output path.
	 */
	public void recordWorkspaceExportPath(@Nonnull Workspace workspace, @Nonnull Path path) {
		WorkspaceModel source = workspaceModelFor(workspace);
		if (source == null)
			return;

		// Update the list of recent workspaces with the new export path.
		String normalizedPath = path.toAbsolutePath().normalize().toString();
		List<WorkspaceModel> updatedList = new ArrayList<>(recentWorkspaces.getValue());
		for (int i = 0; i < updatedList.size(); i++) {
			WorkspaceModel existing = updatedList.get(i);
			if (existing.equalModels(source)) {
				// This model represents the same workspace, so update it with the new export path.
				updatedList.set(i, new WorkspaceModel(source.primary(), source.libraries(), normalizedPath));
				recentWorkspaces.setValue(updatedList);
				return;
			}
		}
		if (maxRecentWorkspaces.getValue() <= 0)
			return;

		// Prune the list to make room for the new workspace.
		while (updatedList.size() >= maxRecentWorkspaces.getValue())
			updatedList.removeLast();

		// Add the workspace with the export path, as it was not already in the list.
		updatedList.addFirst(new WorkspaceModel(source.primary(), source.libraries(), normalizedPath));
		recentWorkspaces.setValue(updatedList);
	}

	/**
	 * @param workspace
	 * 		Workspace to find an export path for.
	 *
	 * @return The latest successful export target path, or {@code null} when the workspace has none
	 * or the workspace cannot be represented as a recent workspace.
	 */
	@Nullable
	public Path getWorkspaceExportPath(@Nonnull Workspace workspace) {
		WorkspaceModel source = workspaceModelFor(workspace);
		if (source == null)
			return null;
		for (WorkspaceModel model : recentWorkspaces.getValue())
			if (model.equalModels(source) && model.exportPath() != null)
				return Paths.get(model.exportPath());
		return null;
	}

	/**
	 * @param workspace
	 * 		Workspace to convert to a recent-workspace model.
	 *
	 * @return Model of the workspace, or {@code null} when the workspace cannot be modeled.
	 */
	@Nullable
	private WorkspaceModel workspaceModelFor(@Nonnull Workspace workspace) {
		// Primary resource must be file or directory based, as other types of resources cannot be represented.
		WorkspaceResource primaryResource = workspace.getPrimaryResource();
		if (!ResourceModel.isSupported(primaryResource))
			return null;

		// Collect supporting resources, filtering out any that cannot be represented.
		// We're going to assume the non-representable resources are generated contents that don't need to be tracked.
		List<ResourceModel> libraries = workspace.getSupportingResources().stream()
				.map(ResourceModel::from)
				.filter(Objects::nonNull)
				.toList();

		ResourceModel primary = ResourceModel.from(primaryResource);
		return new WorkspaceModel(primary, libraries, null);
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
	 * @param exportPath
	 * 		Latest successful output path, or {@code null} when none was recorded.
	 *
	 * @author Matt Coley
	 * @see ResourceModel
	 */
	public record WorkspaceModel(@Nonnull ResourceModel primary,
	                             @Nonnull List<ResourceModel> libraries,
	                             @Nullable String exportPath) {
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

		/**
		 * @param other
		 * 		Other workspace model to compare against.
		 *
		 * @return {@code true} when the two models represent the same workspace resources, ignoring the export path.
		 */
		private boolean equalModels(@Nonnull WorkspaceModel other) {
			return primary().equals(other.primary()) && libraries().equals(other.libraries());
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
			return switch (resource) {
				case WorkspaceFileResource fileResource -> {
					FileInfo fileInfo = fileResource.getFileInfo();
					Path inputPath = InputFilePathProperty.get(fileInfo);
					if (inputPath != null)
						yield new ResourceModel(StringUtil.pathToAbsoluteString(inputPath));
					yield new ResourceModel(fileInfo.getName());
				}
				case WorkspaceDirectoryResource fileResource ->
						new ResourceModel(StringUtil.pathToAbsoluteString(fileResource.getDirectoryPath()));
				case GeneratedPhantomWorkspaceResource generatedPhantomWorkspaceResource -> null;
				default ->
						throw new UnsupportedOperationException("Cannot serialize content source of type: " + resource.getClass().getName());
			};
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
