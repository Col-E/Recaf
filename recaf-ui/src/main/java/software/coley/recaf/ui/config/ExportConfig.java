package software.coley.recaf.ui.config;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableBoolean;
import software.coley.observables.ObservableObject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.workspace.io.WorkspaceCompressType;
import software.coley.recaf.workspace.PathExportingManager;
import software.coley.recaf.workspace.model.Workspace;

/**
 * Config for export options.
 *
 * @author Matt Coley
 * @see PathExportingManager
 */
@ApplicationScoped
public class ExportConfig extends BasicConfigContainer {
	private final ObservableObject<WorkspaceCompressType> compression
			= new ObservableObject<>(WorkspaceCompressType.MATCH_ORIGINAL);
	private final ObservableBoolean bundleSupportingResources = new ObservableBoolean(false);
	private final ObservableBoolean createZipDirEntries = new ObservableBoolean(true);
	private final ObservableBoolean warnNoChanges = new ObservableBoolean(true);

	@Inject
	public ExportConfig() {
		super(ConfigGroups.SERVICE_IO, "export" + CONFIG_SUFFIX);
		// Add values
		addValue(new BasicConfigValue<>("compression", WorkspaceCompressType.class, compression));
		addValue(new BasicConfigValue<>("bundle-supporting-resources", boolean.class, bundleSupportingResources));
		addValue(new BasicConfigValue<>("create-zip-dir-entries", boolean.class, createZipDirEntries));
		addValue(new BasicConfigValue<>("warn-no-changes", boolean.class, warnNoChanges));
	}

	/**
	 * @return Compression type to use when exporting workspaces.
	 */
	@Nonnull
	public ObservableObject<WorkspaceCompressType> getCompression() {
		return compression;
	}

	/**
	 * @return {@code true} to include {@link Workspace#getSupportingResources()} in the output.
	 */
	@Nonnull
	public ObservableBoolean getBundleSupportingResources() {
		return bundleSupportingResources;
	}

	/**
	 * For classes, these entries would be their package names.
	 * This data is not strictly required for a functional ZIP/JAR file, but some software implementations
	 * may expect these entries to exist. For this reason we allow the user to choose if they want to include
	 * this data in outputs.
	 *
	 * @return {@code true} to create ZIP entries for directory paths.
	 */
	@Nonnull
	public ObservableBoolean getCreateZipDirEntries() {
		return createZipDirEntries;
	}

	/**
	 * @return {@code true} to warn users when no changes were made prior to exporting.
	 */
	@Nonnull
	public ObservableBoolean getWarnNoChanges() {
		return warnNoChanges;
	}
}
