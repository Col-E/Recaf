package me.coley.recaf.decompile.fernflower;

import me.coley.recaf.config.ConfDecompile;
import me.coley.recaf.control.Controller;
import me.coley.recaf.decompile.Decompiler;
import me.coley.recaf.util.ClassUtil;
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

	/**
	 * Initialize the decompiler wrapper.
	 *
	 * @param controller
	 * 		Controller with configuration to pull from and the workspace to pull classes from.
	 */
	public FernFlowerDecompiler(Controller controller) {
		super(controller);
		// Initial setup for the given controller
		if (controller.getWorkspace() != null)
			setup(controller.getWorkspace());
	}

	@Override
	protected Map<String, Object> generateDefaultOptions() {
		Map<String, Object> map = new HashMap<>(IFernflowerPreferences.getDefaults());
		map.put("ind", "    ");
		ConfDecompile config = getController().config().decompile();
		if (config.showSynthetic) {
			// FernFlower doesn't have options against intentional marking of ACC_SYNTHETIC by obfuscators :/
			// This will only show ACC_BRIDGE but not ACC_SYNTHETIC
			map.put("rbr", "0"); // hide bridge methods
			map.put("rsy", "0"); // hide synthetic class members
		}
		if (!config.stripDebug) {
			map.put("dgs", "1"); // decompile generics
		}
		return map;
	}

	@Override
	public String decompile(String name) {
		Workspace workspace = getController().getWorkspace();
		// Rerun setup if the workspace has changed.
		// This is required because FernFlower builds a cache of all classes as a custom node structure...
		if (workspace != lastWorkspace)
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
				ConfDecompile config = getController().config().decompile();
				byte[] code = workspace.getRawClass(className);
				if (config.stripDebug)
					code = ClassUtil.stripDebugForDecompile(code);
				return code;
			}
			throw new IllegalStateException("Provider should only receive internal names."+
					"Got external name: " + externalPath);
		};
		decompiler = new FernFlowerAccessor(provider, DUMMY_COLLECTOR, getOptions(), LOGGER);
		try {
			decompiler.addWorkspace(workspace);
			decompiler.analyze();
		} catch(IOException ex) {
			throw new IllegalStateException("Failed to load inputs for FernFlower!", ex);
		} catch(ReflectiveOperationException ex) {
			throw new IllegalStateException("Failed to setup FernFlower!", ex);
		} catch(IndexOutOfBoundsException ex) {
			throw new IllegalStateException("FernFlower internal error", ex);
		} catch(Exception ex) {
			throw new IllegalStateException(ex);
		}
		lastWorkspace = workspace;
	}
}
