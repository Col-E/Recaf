package me.coley.recaf.command.impl;

import me.coley.recaf.command.ControllerCommand;
import me.coley.recaf.command.completion.FileCompletions;
import me.coley.recaf.mapping.MappingImpl;
import me.coley.recaf.mapping.Mappings;
import me.coley.recaf.workspace.JavaResource;
import org.objectweb.asm.ClassReader;
import picocli.CommandLine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static me.coley.recaf.util.Log.info;
import static me.coley.recaf.util.Log.debug;

/**
 * Command for applying mappings.
 *
 * @author Matt
 */
@CommandLine.Command(name = "remap", description = "Apply mappings to the workspace.")
public class Remap extends ControllerCommand implements Callable<Void> {
	@CommandLine.Parameters(index = "0",  description = "The mapping type.", arity = "0..1")
	public MappingImpl mapper = MappingImpl.SIMPLE;
	@CommandLine.Parameters(index = "1",  description = "The mapping file.",
			completionCandidates = FileCompletions.class)
	public Path mapFile;
	@CommandLine.Option(names = "--noDebug", description = "Clear debug info (variable names/generics).")
	public boolean noDebug;
	@CommandLine.Option(names = "--allowLookup",
			description = "Allow hierarchy lookups for inheritance supported mapping. " +
					"Disable for faster mapping if hierarchy is accounted for in the mapping file.",
			defaultValue = "true")
	public boolean lookup = true;

	/**
	 * @return n/a
	 *
	 * @throws Exception
	 * 		<ul><li>IllegalStateException, Invalid map file given</li></ul>
	 */
	@Override
	public Void call() throws Exception {
		if(mapFile == null || !Files.exists(mapFile))
			throw new IllegalStateException("No mapping file provided!");
		// Apply
		Mappings mappings = mapper.create(mapFile, getWorkspace());
		mappings.setClearDebugInfo(noDebug);
		mappings.setCheckFieldHierarchy(lookup);
		mappings.setCheckMethodHierarchy(lookup);

		JavaResource primary = getWorkspace().getPrimary();
		Map<String, byte[]> mapped = mappings.accept(primary);

		byte[] manifestBytes = primary.getFiles().get("META-INF/MANIFEST.MF");
		if (manifestBytes != null) {
			Manifest manifest = new Manifest(new ByteArrayInputStream(manifestBytes));
			Attributes attr = manifest.getMainAttributes();
			if (!attr.isEmpty()) {
				String mainClass = attr.getValue("Main-Class").replaceAll("\\.", "/");
				if (mapped.containsKey(mainClass)) {
					String mappedName = new ClassReader(mapped.get(mainClass))
							.getClassName().replaceAll("/", "\\.");
					debug("Remapping Main-Class attribute in MANIFEST.MF from '{}' to '{}'", mainClass, mappedName);
					attr.putValue("Main-Class", mappedName);
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					manifest.write(outputStream);
					primary.getFiles().put("META-INF/MANIFEST.MF", outputStream.toByteArray());
					outputStream.close();
				}
			}
		}

		// Log
		StringBuilder sb = new StringBuilder("Classes updated: " + mapped.size());
		mapped.forEach((old, value) -> {
			ClassReader reader = new ClassReader(value);
			String rename = reader.getClassName();
			if (!old.equals(rename))
				sb.append("\n - ").append(old).append(" => ").append(rename);
		});
		info(sb.toString());
		return null;
	}
}
