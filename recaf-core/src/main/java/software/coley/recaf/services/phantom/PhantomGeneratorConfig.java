package software.coley.recaf.services.phantom;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableBoolean;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link PhantomGenerator}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class PhantomGeneratorConfig extends BasicConfigContainer implements ServiceConfig {
	private final ObservableBoolean generateWorkspacePhantoms = new ObservableBoolean(false);
	private final ObservableBoolean lenientConflictingHierarchies = new ObservableBoolean(true);

	@Inject
	public PhantomGeneratorConfig() {
		super(ConfigGroups.SERVICE_ANALYSIS, PhantomGenerator.SERVICE_ID + CONFIG_SUFFIX);
		addValue(new BasicConfigValue<>("generate-workspace-phantoms", boolean.class, generateWorkspacePhantoms));
		addValue(new BasicConfigValue<>("lenient-conflicting-hierarchies", boolean.class, lenientConflictingHierarchies));
	}

	/**
	 * @return {@code true} to create and register {@link GeneratedPhantomWorkspaceResource} to newly opened workspaces.
	 */
	@Nonnull
	public ObservableBoolean getGenerateWorkspacePhantoms() {
		return generateWorkspacePhantoms;
	}

	/**
	 * @return {@code true} to resolve incompatible phantom superclass constraints in a compile-first lenient mode.
	 */
	@Nonnull
	public ObservableBoolean getLenientConflictingHierarchies() {
		return lenientConflictingHierarchies;
	}
}
