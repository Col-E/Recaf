package me.coley.recaf.util;

import me.coley.recaf.Controller;
import me.coley.recaf.config.Configs;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.WorkspaceListener;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.source.JarContentSource;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The way that Recaf makes decompile-recompile work is by adding all the code in the workspace
 * to the compiler classpath. Plus then any missing data is generated <i>(Phantom classes of library
 * code the user has not specified)</i> and also adding that to the classpath.
 * <br>
 * This utility registers listeners that ensure this information is generated and in the compiler classpath directory.
 * To save space it also removes lots of information we don't need if we're just using it to compile against.
 *
 * @author Matt Coley
 */
public class CompileDependencyUpdater {
	private static final Logger logger = Logging.get(CompileDependencyUpdater.class);
	private static final String PRIMARY_NAME = "compiler_cached_primary.jar";
	private static final ClearableThreadPool phantomThreadPool = new ClearableThreadPool(1, true, "Phantom generation");

	/**
	 * @param controller
	 * 		Controller to register listeners with.
	 */
	public static void install(Controller controller) {
		controller.addListener((oldWorkspace, newWorkspace) -> {
			Threads.run(() -> {
				if (newWorkspace != null) {
					Resource primary = newWorkspace.getResources().getPrimary();
					// Clear old data when a new workspace is opened
					clear();
					// Copy over files to directory
					try {
						writeResource(primary, PRIMARY_NAME);
						for (Resource resource : newWorkspace.getResources().getLibraries()) {
							writeResource(resource);
						}
					} catch (IOException ex) {
						logger.error("Failed copying resources to compiler classpath directory", ex);
					}
					// Any time a library is added, add it to the classpath directory
					newWorkspace.addListener(new WorkspaceListener() {
						@Override
						public void onAddLibrary(Workspace workspace, Resource library) {
							try {
								logger.debug("Library added to workspace, updating compiler cache...");
								writeResource(library);
							} catch (IOException ex) {
								logger.error("Failed writing resource to compiler classpath directory", ex);
							}
						}

						@Override
						public void onRemoveLibrary(Workspace workspace, Resource library) {
							// no-op
						}
					});
					// Analyze and create phantoms. Abandon any prior analysis.
					if (phantomThreadPool.hasActiveThreads())
						phantomThreadPool.clear();
					phantomThreadPool.submit(() -> createPhantoms(newWorkspace));
					// I don't think it will be necessary to rewrite the primary jar for every edit.
					// For now until somebody can think of a good use case without too much overhead, we won't do that.
				}
			});
		});
		// Any time mappings update we need to update the primary jar
		controller.getServices().getMappingsManager().addAggregatedMappingsListener(mappings -> Threads.run(() -> {
			try {
				logger.debug("Mappings have been updated, updating compiler cache...");
				writePrimary(controller);
			} catch (IOException ex) {
				logger.error("Failed writing primary resource to compiler classpath directory", ex);
			}
		}));
	}

	/**
	 * Analyzes code in the workspace and generates a jar with missing data.
	 *
	 * @param workspace
	 * 		Workspace to analyze.
	 */
	private static void createPhantoms(Workspace workspace) {
		// TODO: Allow an opt-in feature where phantom classes are not deleted.
		//  - Means if you re-open the same resource, it looks for the cached first
		if (!Configs.compiler().generatePhantoms) {
			return;
		}
		Path target = Directories.getPhantomsDirectory().resolve("phantoms.jar");
		Resource primary = workspace.getResources().getPrimary();
		try {
			Map<String, byte[]> map = JPhantomUtil.generate(primary.getClasses());
			if (map.isEmpty()) {
				logger.info("No phantom classes were generated, no errors reported");
				return;
			} else {
				logger.info("{} phantom classes were generated", map.size());
			}
			// Write
			Exporter exporter = new Exporter(target);
			exporter.addRawClasses(map);
			exporter.compress = true;
			exporter.hollowClasses = false;
			exporter.writeAsArchive();
			// Add to workspace
			JarContentSource source = new JarContentSource(target);
			Resource resource = new Resource(source);
			resource.read();
			workspace.getResources().getInternalLibraries().add(resource);
		} catch (Exception e) {
			logger.error("Failed to generate phantom classes", e);
		}
	}

	/**
	 * @param controller
	 * 		Controller to pull workspace from.
	 *
	 * @throws IOException
	 * 		When the primary jar cannot be written to the compiler classpath directory.
	 */
	private static void writePrimary(Controller controller) throws IOException {
		Workspace workspace = controller.getWorkspace();
		if (workspace == null)
			return;
		writeResource(workspace.getResources().getPrimary(), PRIMARY_NAME);
	}

	/**
	 * @param resource
	 * 		Resource to write.
	 *
	 * @throws IOException
	 * 		When the resource cannot be written to the compiler classpath directory.
	 */
	private static void writeResource(Resource resource) throws IOException {
		writeResource(resource, "compiler_cached_lib_" + Math.abs(resource.hashCode()) + ".jar");
	}

	/**
	 * @param resource
	 * 		Resource to write.
	 * @param name
	 * 		Name to apply to the resource in the classpath directory.
	 *
	 * @throws IOException
	 * 		When the resource cannot be written to the compiler classpath directory.
	 */
	private static void writeResource(Resource resource, String name) throws IOException {
		Path classpathDir = Directories.getClasspathDirectory();
		Path output = classpathDir.resolve(name);
		// Setup exporter
		Exporter exporter = new Exporter(output);
		exporter.compress = true;
		exporter.hollowClasses = true;
		exporter.skipFiles = true;
		exporter.shadeLibs = false;
		// Write resource
		exporter.addResource(resource);
		exporter.writeAsArchive();
	}

	/**
	 * Removes all content in the classpath and phantoms directories.
	 */
	private static void clear() {
		Path classpathDir = Directories.getClasspathDirectory();
		Path phantomsDir = Directories.getPhantomsDirectory();
		// Cleanup anything that exists on startup
		try {
			deleteDir(classpathDir);
			deleteDir(phantomsDir);
		} catch (IOException ex) {
			logger.error("Failed to cleanup compiler classpath directories!", ex);
		}
	}

	/**
	 * @param rootPath
	 * 		Directory with stuff to yeet.
	 *
	 * @throws IOException
	 * 		When file traversal fails.
	 */
	private static void deleteDir(Path rootPath) throws IOException {
		if (!Files.isDirectory(rootPath))
			return;
		try (Stream<Path> walk = Files.walk(rootPath)) {
			walk.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(File::delete);
		}
	}
}
