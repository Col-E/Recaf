package me.coley.recaf.decompile.fernflower;

import me.coley.recaf.decompile.Decompiler;
import me.coley.recaf.workspace.Workspace;
import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;

import java.io.*;
import java.util.*;

/**
 * FernFlower decompiler implementation.
 *
 * @author Matt
 */
public class FernFlowerDecompiler extends Decompiler<Object> {
	private static final FernFlowerLogger LOGGER = new FernFlowerLogger();
	private static final DummyCollector DUMMY_COLLECTOR = new DummyCollector();
	private FernFlowerAccessor decompiler;
	private Workspace lastWorkspace;

	@Override
	protected Map<String, Object> generateDefaultOptions() {
		Map<String, Object> map = new HashMap<>(IFernflowerPreferences.getDefaults());
		map.put("ind", "    ");
		return map;
	}

	@Override
	public String decompile(Workspace workspace, String name) {
		// Setup FernFlower if it's not already setup.
		// Don't reset FernFlower unless the workspace is different to save time on class analysis.
		// (FernFlower builds a cache of all classes as a custom node structure)
		if (decompiler == null || workspace != lastWorkspace)
			setup(workspace);
		// Dump class content
		return decompiler.decompile(name);
	}

	/**
	 * Initialize FernFlower with the given workspace.
	 *
	 * @param workspace
	 * 		Workspace to pull classes from.
	 */
	private void setup(Workspace workspace) {
		IBytecodeProvider provider = (externalPath, internalPath) -> {
			if(internalPath != null) {
				String className = internalPath.substring(0, internalPath.indexOf(".class"));
				return workspace.getRawClass(className);
			}
			throw new IllegalStateException("Provider should only receive internal names."+
					"Got external name: " + externalPath);
		};
		decompiler = new FernFlowerAccessor(provider, DUMMY_COLLECTOR, getOptions(), LOGGER);
		try {
			decompiler.addWorkspace(workspace);
		} catch(IOException ex) {
			throw new IllegalStateException("Failed to load inputs for FernFlower!", ex);
		} catch(ReflectiveOperationException ex) {
			throw new IllegalStateException("Failed to setup FernFlower!", ex);
		} catch(IndexOutOfBoundsException ex) {
			throw new IllegalStateException("FernFlower internal error", ex);
		} catch(Exception ex) {
			throw new IllegalStateException(ex);
		}
		decompiler.analyze();
		lastWorkspace = workspace;
	}
}
