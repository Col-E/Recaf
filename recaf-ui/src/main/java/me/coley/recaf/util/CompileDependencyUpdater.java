package me.coley.recaf.util;

import me.coley.recaf.Controller;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.ClassSourceType;
import me.coley.recaf.config.Configs;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.source.EmptyContentSource;
import org.slf4j.Logger;

import java.util.Map;

/**
 * The way that Recaf makes decompile-recompile work is by adding all the code in the workspace
 * to the compiler classpath. Plus then any missing data is generated <i>(Phantom classes of library
 * code the user has not specified)</i> and also adding that to the classpath.
 * <br>
 * This utility registers listeners that ensure this information is generated.
 *
 * @author Matt Coley
 */
public class CompileDependencyUpdater {
	private static final Logger logger = Logging.get(CompileDependencyUpdater.class);
	private static final ClearableThreadPool phantomThreadPool = new ClearableThreadPool(1, true, "Phantom generation");

	/**
	 * @param controller
	 * 		Controller to register listeners with.
	 */
	public static void install(Controller controller) {
		controller.addListener((oldWorkspace, newWorkspace) -> {
			if (newWorkspace != null) {
				// Analyze and create phantoms. Abandon any prior analysis.
				if (phantomThreadPool.hasActiveThreads())
					phantomThreadPool.clear();
				phantomThreadPool.submit(() -> createPhantoms(newWorkspace));
			}
		});
	}

	/**
	 * Analyzes code in the workspace and generates a jar with missing data.
	 *
	 * @param workspace
	 * 		Workspace to analyze.
	 */
	private static void createPhantoms(Workspace workspace) {
		if (!Configs.compiler().generatePhantoms) {
			return;
		}
		Resource primary = workspace.getResources().getPrimary();
		try {
			logger.info("Attempting to generate phantoms, analyzing {} classes...", primary.getClasses().size());
			Map<String, byte[]> map = JPhantomUtil.generate(primary.getClasses());
			if (map.isEmpty()) {
				logger.info("No phantom classes were generated, no errors reported");
				return;
			} else {
				logger.info("{} phantom classes were generated", map.size());
			}
			// Add to workspace
			Resource resource = new Resource(new EmptyContentSource());
			map.forEach((name, bytes) -> resource.getClasses().put(ClassInfo.read(bytes, ClassSourceType.PHANTOM_GENERATED)));
			workspace.getResources().getInternalLibraries().add(resource);
		} catch (Exception e) {
			logger.error("Failed to generate phantom classes", e);
		}
	}
}
