package me.coley.recaf.command.impl;

import me.coley.recaf.command.ControllerCommand;
import me.coley.recaf.command.completion.WorkspaceNameCompletions;
import me.coley.recaf.decompile.DecompileImpl;
import me.coley.recaf.decompile.Decompiler;
import picocli.CommandLine;

import java.util.*;
import java.util.concurrent.Callable;

/**
 * Command for decompiling a class in the workspace.
 *
 * @author Matt
 */
@CommandLine.Command(name = "decompile", description = "Decompile a class in the workspace.")
public class Decompile extends ControllerCommand implements Callable<String> {
	@CommandLine.Option(names = {"--decompiler"}, description = "The decompiler implementation to use.",
			defaultValue = "CFR")
	public DecompileImpl decompiler = DecompileImpl.CFR;
	@CommandLine.Parameters(index = "0",  description = "The class to decompile",
			completionCandidates = WorkspaceNameCompletions.class)
	public String className;
	@CommandLine.Option(names = { "--options" },  description = "List of options to pass.", arity = "0..*")
	public Map<String, String> options = new HashMap<>();

	/**
	 * @return n/a
	 *
	 * @throws Exception
	 * 		<ul><li>IllegalStateException, cannot find class</li><li>Other, decompiler
	 * 		error</li></ul>
	 */
	@Override
	@SuppressWarnings("unchecked")
	public String call() throws Exception {
		if(className == null || className.isEmpty())
			throw new IllegalStateException("No class specified to decompile");
		if(!getWorkspace().hasClass(className))
			throw new IllegalStateException("No class by the name '" + className +
					"' exists in the workspace");
		String prefix = (getController().config().decompile().showName ?
				"// Decompiled with: " + decompiler.getNameAndVersion() + "\n" : "");
		Decompiler<?> impl = decompiler.create(getController());
		impl.getOptions().putAll((Map) options);
		return prefix + impl.decompile(className);
	}
}
