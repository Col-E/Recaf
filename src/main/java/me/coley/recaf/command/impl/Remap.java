package me.coley.recaf.command.impl;

import me.coley.recaf.command.completion.FileCompletions;
import me.coley.recaf.mapping.*;
import org.objectweb.asm.ClassReader;
import picocli.CommandLine;

import java.io.File;
import java.util.Map;
import java.util.concurrent.Callable;

import static me.coley.recaf.util.Log.*;

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
	public File mapFile;
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
		if(mapFile == null || !mapFile.isFile())
			throw new IllegalStateException("No mapping file provided!");
		// Apply
		Mappings mappings = mapper.create(mapFile, getWorkspace());
		mappings.setClearDebugInfo(noDebug);
		mappings.setCheckFieldHierarchy(lookup);
		mappings.setCheckMethodHierarchy(lookup);
		Map<String, byte[]> mapped = mappings.accept(getWorkspace().getPrimary());
		// TODO: If the primary has a "META-INF/MANIFEST.MF" update the main class if renamed
		// Log
		StringBuilder sb = new StringBuilder("Classes updated: " + mapped.size());
		mapped.forEach((old, value) -> {
			ClassReader reader = new ClassReader(value);
			String rename = reader.getClassName();
			sb.append("\n - ").append(old);
			if (!old.equals(rename))
				sb.append(" => ").append(rename);
		});
		info(sb.toString());
		return null;
	}
}
