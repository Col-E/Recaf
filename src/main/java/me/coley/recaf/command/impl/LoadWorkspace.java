package me.coley.recaf.command.impl;

import me.coley.recaf.command.completion.*;
import me.coley.recaf.util.IOUtil;
import me.coley.recaf.util.LangUtil;
import me.coley.recaf.util.ShortcutUtil;
import me.coley.recaf.workspace.*;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;

import static me.coley.recaf.util.Log.*;

/**
 * Command for loading the a workspace from a file.
 *
 * @author Matt
 */
@CommandLine.Command(name = "loadworkspace", description = "Loads a workspace from a resource or workspace config.")
public class LoadWorkspace implements Callable<Workspace> {
	@CommandLine.Parameters(index = "0",  description = "The file to load. " +
			"Supported types are: class, jar, json", completionCandidates = WorkspaceFileCompletions.class)
	public Path input;
	@CommandLine.Option(names = { "--sources" },  description = "Archive containing sources of the resource.",
			completionCandidates = ArchiveFileCompletions.class)
	public Path sources;
	@CommandLine.Option(names = { "--docs" },  description = "Archive containing javadocs of the resource.",
			completionCandidates = ArchiveFileCompletions.class)
	public Path javadoc;
	@CommandLine.Option(names = { "--lazy" },  description = "Don't immediately load the workspace content.")
	public boolean lazy;
	@CommandLine.Option(names = "--skip")
	public List<String> skippedPrefixes;
	private String status = "...";

	@Override
	public Workspace call() throws Exception {
		status = LangUtil.translate("ui.load.resolve");
		String name = input.getFileName().toString().toLowerCase();
		// Handle symbolic links
		int symLevel = 0;
		if (ShortcutUtil.isPotentialValidLink(input)) {
			input = Paths.get(new ShortcutUtil(input).getRealFilename());
			name = input.getFileName().toString().toLowerCase();
		}
		while (Files.isSymbolicLink(input) && symLevel < 5) {
			input = Files.readSymbolicLink(input);
			symLevel++;
		}
		String ext;
		if (Files.isDirectory(input)) {
			ext = "dir";
		} else {
			ext = IOUtil.detectExtension(input);
		}
		JavaResource resource;
		switch(ext) {
			case "class":
				status = LangUtil.translate("ui.load.initialize.resource");
				resource = new ClassResource(input);
				break;
			case "zip":
			case "jar":
				status = LangUtil.translate("ui.load.initialize.resource");
				resource = new JarResource(input);
				break;
			case "war":
				status = LangUtil.translate("ui.load.initialize.resource");
				resource = new WarResource(input);
				break;
			case "dir":
				status = LangUtil.translate("ui.load.initialize.resource");
				resource = new DirectoryResource(input);
				break;
			case "json":
				status = LangUtil.translate("ui.load.initialize.workspace");
				// Represents an already existing workspace, so we can parse and return that here
				Workspace workspace = null;
				try {
					workspace = WorkspaceIO.fromJson(input);
				} catch(Exception ex) {
					throw new IllegalArgumentException("Failed to parse workspace config '" + name + "'", ex);
				}
				workspace.analyzePhantoms();
				// Initial load classes & files
				if (!lazy) {
					status = LangUtil.translate("ui.load.loading");
					workspace.getPrimary().getClasses();
					workspace.getPrimary().getFiles();
				}
				info("Loaded workspace from: {}", input.getFileName());
				return workspace;
			default:
				throw new IllegalArgumentException("Unsupported file type '" + ext + "'");
		}
		//
		if (skippedPrefixes != null)
			resource.setSkippedPrefixes(skippedPrefixes);
		// Initial load classes & files
		if (!lazy) {
			status = LangUtil.translate("ui.load.loading");
			resource.setPrimary(true);
			resource.getClasses();
			resource.getFiles();
		}
		// Load sources/javadoc if present
		status = LangUtil.translate("ui.load.srcdocs");
		if (sources != null && Files.exists(sources))
			resource.setClassSources(sources);
		if (javadoc != null && Files.exists(javadoc))
			resource.setClassDocs(javadoc);
		// Create workspace
		Workspace workspace = new Workspace(resource);
		workspace.analyzePhantoms();
		status = LangUtil.translate("ui.load.done");
		info("Loaded workspace from: {}", input.getFileName());
		return workspace;
	}

	/**
	 * Used for UI progress reporting.
	 *
	 * @return Current load status.
	 */
	public String getStatus() {
		return status;
	}
}
