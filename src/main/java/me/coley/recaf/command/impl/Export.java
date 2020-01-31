package me.coley.recaf.command.impl;

import me.coley.recaf.workspace.*;
import org.apache.commons.io.FileUtils;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

import static me.coley.recaf.util.Log.*;

/**
 * Command for outputting workspace resources.
 *
 * @author Matt
 */
@CommandLine.Command(name = "export", description = "Export workspace to a class/jar.")
public class Export extends WorkspaceCommand implements Callable<Void> {
	@CommandLine.Parameters(index = "0",  description = "The output file.")
	public File output;
	@CommandLine.Option(names = { "--shadelibs" }, description = "Add library files to export.")
	public boolean shadeLibs;

	/**
	 * @return n/a
	 *
	 * @throws Exception
	 * 		<ul><li>IOException, cannot write to output</li></ul>
	 */
	@Override
	public Void call() throws Exception {
		// Ensure parent directory exists
		File parentDir = output.getParentFile();
		if (parentDir != null && !parentDir.isDirectory() && !parentDir.mkdirs())
			throw new IOException("Failed to create parent directory for: " + output);
		JavaResource primary = workspace.getPrimary();
		// Handle class exports
		boolean noShadeContent = !shadeLibs || workspace.getLibraries().isEmpty();
		if (primary instanceof ClassResource && noShadeContent) {
			byte[] clazz = primary.getClasses().values().iterator().next();
			FileUtils.writeByteArrayToFile(output, clazz);
			info("Saved to {}", output.getName());
			return null;
		}
		// Collect content to put into export archive
		Map<String, byte[]> outContent = new TreeMap<>();
		if (shadeLibs)
			workspace.getLibraries().forEach(lib -> put(outContent, lib));
		put(outContent, primary);
		// Calculate modified classes
		Set<String> modifiedClasses = new HashSet<>();
		Set<String> modifiedResources = new HashSet<>();
		modifiedClasses.addAll(primary.getDirtyClasses());
		modifiedClasses.addAll(primary.getClassHistory().entrySet().stream()
				.filter(e -> e.getValue().size() > 1)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet()));
		modifiedResources.addAll(primary.getFileHistory().entrySet().stream()
				.filter(e -> e.getValue().size() > 1)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet()));
		// Write to archive
		if (output.isDirectory() && primary instanceof DirectoryResource)
			writeDirectory(outContent);
		else
			writeArchive(outContent);
		info("Saved to {}.\n - Modified classes: {}\n - Modified resources: {}",
				output.getName(), modifiedClasses.size(), modifiedResources.size());
		return null;
	}

	private void writeDirectory(Map<String, byte[]> content) throws IOException {
		for (Map.Entry<String, byte[]> entry : content.entrySet()) {
			Path path = Paths.get(output.getAbsolutePath(), entry.getKey());
			Files.createDirectories(path.getParent());
			Files.write(path, entry.getValue());
		}
	}

	private void writeArchive(Map<String, byte[]> archiveContent) throws IOException {
		try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(output))) {
			Set<String> dirsVisited = new HashSet<>();
			// Contents is iterated in sorted order (because 'archiveContent' is TreeMap).
			// This allows us to insert directory entries before file entries of that directory occur.
			for (Map.Entry<String, byte[]> entry : archiveContent.entrySet()) {
				String key = entry.getKey();
				// Write directories for upcoming entries if necessary
				// - Ugly, but does the job.
				if (key.contains("/")) {
					// Record directories
					String parent = key;
					List<String> toAdd = new ArrayList<>();
					do {
						parent = parent.substring(0, parent.lastIndexOf('/'));
						if (dirsVisited.add(parent)) {
							toAdd.add(0, parent + '/');
						} else break;
					} while (parent.contains("/"));
					// Put directories in order of depth
					for (String dir : toAdd) {
						jos.putNextEntry(new JarEntry(dir));
						jos.closeEntry();
					}
				}
				// Write entry content
				jos.putNextEntry(new JarEntry(key));
				jos.write(entry.getValue());
				jos.closeEntry();
			}
		}
	}

	private void put(Map<String, byte[]> content, JavaResource res) {
		content.putAll(res.getFiles());
		for(Map.Entry<String, byte[]> e : res.getClasses().entrySet()) {
			String name = e.getKey() + ".class";
			// War files have a required prefix
			if(res instanceof WarResource)
				name = WarResource.WAR_CLASS_PREFIX + name;
			content.put(name, e.getValue());
		}
	}
}
