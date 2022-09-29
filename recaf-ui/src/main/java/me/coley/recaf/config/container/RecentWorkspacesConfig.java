package me.coley.recaf.config.container;

import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.config.ConfigID;
import me.coley.recaf.config.bounds.IntBounds;
import me.coley.recaf.ui.control.tree.WorkspaceTreeWrapper;
import me.coley.recaf.ui.pane.WorkspacePane;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.ResourceIO;
import me.coley.recaf.workspace.resource.Resources;
import me.coley.recaf.workspace.resource.source.*;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Config container for recent workspaces.
 *
 * @author Matt Coley
 */
public class RecentWorkspacesConfig implements ConfigContainer {
	private static final Logger logger = Logging.get(RecentWorkspacesConfig.class);
	/**
	 * Max number of items to track in {@link #recentWorkspaces}.
	 */
	@IntBounds(min = 1, max = 20)
	@ConfigID("max")
	public int max = 10;
	/**
	 * Representations of recent workspaces.
	 */
	@ConfigID("workspaces")
	public List<WorkspaceModel> recentWorkspaces = new ArrayList<>();

	@Override
	public boolean isInternal() {
		return true;
	}

	@Override
	public String iconPath() {
		return Icons.RECENT;
	}

	@Override
	public String internalName() {
		return "conf.recent";
	}

	/**
	 * Ensure loaded values in {@link #recentWorkspaces} are valid.
	 * Invalid items are removed.
	 */
	public void update() {
		List<WorkspaceModel> invalidModels = recentWorkspaces.stream()
				.filter(m -> !m.canLoadWorkspace())
				.collect(Collectors.toList());
		// Prune items that cannot be loaded anymore
		if (!invalidModels.isEmpty()) {
			int i = invalidModels.size();
			recentWorkspaces.removeAll(invalidModels);
			logger.warn("Removed {} items from the recent workspaces list. Content sources could not be loaded.", i);
		}
		// Prune items beyond max
		while (recentWorkspaces.size() > max) {
			recentWorkspaces.remove(recentWorkspaces.size() - 1);
		}
	}

	/**
	 * @param workspace
	 * 		Workspace to check for serialization compatibility.
	 *
	 * @return {@code true} when can be used in {@link #addWorkspace(Workspace)}.
	 */
	public boolean canSerialize(Workspace workspace) {
		try {
			WorkspaceModel.from(workspace);
			return true;
		} catch (UnsupportedOperationException ignored) {
			return false;
		}
	}

	/**
	 * @param workspace
	 * 		Workspace to add to the recent list.
	 */
	public void addWorkspace(Workspace workspace) {
		WorkspaceModel model = RecentWorkspacesConfig.WorkspaceModel.from(workspace);
		recentWorkspaces.remove(model);
		recentWorkspaces.add(0, model);
		update();
	}

	/**
	 * Basic wrapper for workspaces.
	 *
	 * @author Matt Coley
	 * @see ResourceModel
	 */
	public static class WorkspaceModel {
		private final ResourceModel primary;
		private final List<ResourceModel> libraries;

		/**
		 * @param primary
		 * 		Primary resource of the workspace.
		 * @param libraries
		 * 		Workspace supporting libraries.
		 */
		public WorkspaceModel(ResourceModel primary, List<ResourceModel> libraries) {
			this.primary = primary;
			this.libraries = libraries;
		}

		/**
		 * @return Primary resource of the workspace.
		 */
		public ResourceModel getPrimary() {
			return primary;
		}

		/**
		 * @return Workspace supporting libraries.
		 */
		public List<ResourceModel> getLibraries() {
			return libraries;
		}

		/**
		 * @return {@code true} when the files still exist at their expected locations.
		 */
		public boolean canLoadWorkspace() {
			Path path = Paths.get(getPrimary().getPath());
			if (!Files.exists(path))
				return false;
			for (ResourceModel model : getLibraries()) {
				path = Paths.get(model.getPath());
				if (!Files.exists(path))
					return false;
			}
			return true;
		}

		/**
		 * @param workspace
		 * 		Workspace with resources to convert.
		 *
		 * @return Representation of the workspace resources.
		 */
		public static WorkspaceModel from(Workspace workspace) {
			ResourceModel primary = ResourceModel.from(workspace.getResources().getPrimary());
			List<ResourceModel> libraries = new ArrayList<>();
			for (Resource library : workspace.getResources().getLibraries()) {
				libraries.add(ResourceModel.from(library));
			}
			return new WorkspaceModel(primary, libraries);
		}

		/**
		 * @return Workspace loaded from the file paths.
		 *
		 * @throws IOException
		 * 		When one of the resources cannot be read from.
		 */
		public Workspace loadWorkspace() throws IOException {
			WorkspaceTreeWrapper wrapper = WorkspacePane.getInstance().getTree();
			try {
				// Update overlay
				List<Path> files = new ArrayList<>();
				files.add(Paths.get(getPrimary().getPath()));
				files.addAll(getLibraries().stream().map(m -> Paths.get(m.getPath())).collect(Collectors.toList()));
				wrapper.addLoadingOverlay(files);
				// Load from paths
				Resource primary = parse(getPrimary().getPath());
				List<Resource> libraries = new ArrayList<>();
				for (ResourceModel model : getLibraries()) {
					Resource library = parse(model.getPath());
					libraries.add(library);
				}
				// Clear overlay
				wrapper.clearOverlay();
				// Wrap and return
				Resources resources = new Resources(primary, libraries);
				return new Workspace(resources);
			} catch (IOException ex) {
				// Clear overlay, pass exception to callee handler
				wrapper.clearOverlay();
				throw ex;
			}
		}

		private static Resource parse(String pathStr) throws IOException {
			// URL check
			if (pathStr.contains("://")) {
				return new Resource(new UrlContentSource(new URL(pathStr)));
			}
			// Maven check
			String[] sections = pathStr.split(":");
			if (sections.length == 3) {
				return new Resource(new MavenContentSource(sections[0], sections[1], sections[2]));
			} else if (sections.length == 4) {
				return new Resource(new MavenContentSource(sections[0], sections[1], sections[2], sections[3]));
			}
			// Everything else should be a normal file or directory
			Path path = Paths.get(pathStr);
			return ResourceIO.fromPath(path, true);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			WorkspaceModel that = (WorkspaceModel) o;
			return primary.equals(that.primary) && libraries.equals(that.libraries);
		}

		@Override
		public int hashCode() {
			return Objects.hash(primary, libraries);
		}
	}

	/**
	 * Wrapper for a resources content source path.
	 *
	 * @author Matt Coley
	 */
	public static class ResourceModel {
		private final String path;

		/**
		 * @param path
		 * 		Path to the resource content source.
		 */
		public ResourceModel(String path) {
			this.path = path;
		}

		/**
		 * @param resource
		 * 		Some resource with a supported {@link ContentSource} type to transform into a {@code String}.
		 *
		 * @return Representation of the content source.
		 */
		public static ResourceModel from(Resource resource) {
			ContentSource source = resource.getContentSource();
			SourceType type = source.getType();
			switch (type) {
				case JAR:
				case WAR:
				case APK:
				case ZIP:
				case JMOD:
				case MODULES:
				case DIRECTORY:
				case SINGLE_FILE:
					FileContentSource fileSource = (FileContentSource) source;
					return new ResourceModel(fileSource.getPath().toAbsolutePath().toString());
				case MAVEN:
					MavenContentSource mavenSource = (MavenContentSource) source;
					return new ResourceModel(mavenSource.getArtifactCoordinates());
				case URL:
					UrlContentSource urlSource = (UrlContentSource) source;
					return new ResourceModel(urlSource.getUrl());
				case INSTRUMENTATION:
				case EMPTY:
				default:
					throw new UnsupportedOperationException("Cannot serialize content source of type: " + type);
			}
		}

		/**
		 * @return Shortened path of resource's content source.
		 */
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
		public String getPath() {
			return path;
		}


		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ResourceModel that = (ResourceModel) o;
			return path.equals(that.path);
		}

		@Override
		public int hashCode() {
			return Objects.hash(path);
		}
	}
}
