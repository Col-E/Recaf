package me.coley.recaf.command.impl;

import me.coley.recaf.mapping.*;
import org.objectweb.asm.ClassReader;
import org.tinylog.Logger;
import picocli.CommandLine;

import java.io.File;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Command for applying mappings.
 *
 * @author Matt
 */
@CommandLine.Command(name = "remap", description = "Apply mappings to the workspace.")
public class Remap extends WorkspaceCommand implements Callable<Void> {
	@CommandLine.Parameters(index = "0",  description = "The mapping type.", arity = "0..1")
	public MappingImpl mapper = MappingImpl.SIMPLE;
	@CommandLine.Parameters(index = "1",  description = "The mapping file.")
	public File mapFile;

	/**
	 * @return n/a
	 *
	 * @throws Exception
	 * 		<ul><li>IllegalStateException, Invalid map file given</li></ul>
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Void call() throws Exception {
		if(mapFile == null || !mapFile.isFile())
			throw new IllegalStateException("No mapping file provided!");
		// Apply
		Mappings mappings = mapper.create(mapFile);
		Map<String, byte[]> mapped = mappings.accept(workspace.getPrimary());
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
		Logger.info(sb.toString());
		return null;
	}
}
