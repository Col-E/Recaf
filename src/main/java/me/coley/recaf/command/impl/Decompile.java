package me.coley.recaf.command.impl;

import me.coley.recaf.decompile.DecompileImpl;
import me.coley.recaf.decompile.Decompiler;
import me.coley.recaf.workspace.*;
import org.tinylog.Logger;
import picocli.CommandLine;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Command for decompiling a class in the workspace.
 *
 * @author Matt
 */
@CommandLine.Command(name = "decompile", description = "Decompile a class in the workspace.")
public class Decompile extends WorkspaceCommand implements Callable<String> {
	@CommandLine.Parameters(index = "0",  description = "The decompiler implementation to use.", arity = "0..1")
	public DecompileImpl decompiler = DecompileImpl.CFR;
	@CommandLine.Parameters(index = "1",  description = "The class to decompile")
	public String className;
	@CommandLine.Option(names = { "--options" },  description = "List of options to pass.", arity = "0..*")
	public Map<String, String> options = new HashMap<>();

	@Override
	@SuppressWarnings("unchecked")
	public String call() throws Exception {
		if(className == null || className.isEmpty())
			throw new IllegalStateException("No class specified to decompile");
		if(!workspace.hasClass(className))
			throw new IllegalStateException("No class by the name '" + className +
					"' exists in the workspace");
		Decompiler<?> impl = decompiler.create();
		impl.getOptions().putAll((Map) options);
		return impl.decompile(workspace, className);
	}
}
