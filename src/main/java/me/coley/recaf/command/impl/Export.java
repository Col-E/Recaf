package me.coley.recaf.command.impl;

import me.coley.recaf.workspace.*;
import org.apache.commons.io.FileUtils;
import picocli.CommandLine;

import java.io.*;
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
		if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs())
			throw new IOException("Failed to create parent directory for: " + output);
		JavaResource primary = workspace.getPrimary();
		// Determine kind of export
		ResourceKind kind = primary.getKind();
		if (kind == ResourceKind.URL)
			kind = ((DeferringResource)primary).getBacking().getKind();
		boolean exportClass = (!shadeLibs && workspace.getLibraries().size() > 0) && kind == ResourceKind.CLASS;
		// Class export
		if (exportClass) {
			byte[] clazz = primary.getClasses().values().iterator().next();
			FileUtils.writeByteArrayToFile(output, clazz);
			info("Saved to {}", output.getName());
			return null;
		}
		// Collect content to put into export jar
		Map<String, byte[]> jarContent = new TreeMap<>();
		if (shadeLibs)
			workspace.getLibraries().forEach(lib -> put(jarContent, lib));
		put(jarContent, primary);
		// Calculate modified classes
		Set<String> modified = new HashSet<>();
		modified.addAll(primary.getDirtyClasses());
		modified.addAll(primary.getClassHistory().entrySet().stream()
				.filter(e -> e.getValue().size() > 1)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet()));
		// Write to jar
		try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(output))) {
			Set<String> dirsVisited = new HashSet<>();
			// Contents is iterated in sorted order (because 'jarContent' is TreeMap).
			// This allows us to insert directory entries before file entries of that directory occur.
			for (Map.Entry<String, byte[]> entry : jarContent.entrySet()) {
				String key = entry.getKey();
				// Write directories for upcoming entries if necessary
				// - Ugly, but does the job.
				if (key.contains("/")) {
					// Record directories
					String parent = key;
					List<String> toAdd = new ArrayList<>();
					do {
						parent = parent.substring(0, parent.lastIndexOf("/"));
						if (!dirsVisited.contains(parent)) {
							dirsVisited.add(parent);
							toAdd.add(0, parent + "/");
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
		info("Saved to {}.\n - Modified classes: {}", output.getName(), modified.size());
		return null;
	}

	private void put(Map<String,byte[]> content, JavaResource res) {
		content.putAll(res.getResources());
		for (Map.Entry<String, byte[]> e : res.getClasses().entrySet())
			content.put(e.getKey() + ".class", e.getValue());
	}
}
