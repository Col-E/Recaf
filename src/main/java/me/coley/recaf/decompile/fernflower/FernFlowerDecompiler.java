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
	@Override
	protected Map<String, Object> generateDefaultOptions() {
		Map<String, Object> map = new HashMap<>(IFernflowerPreferences.getDefaults());
		map.put("ind", "\t");
		return map;
	}

	@Override
	public String decompile(Workspace workspace, String name) {
		FernFlowerLogger logger = new FernFlowerLogger();
		DummyCollector collector = new DummyCollector();
		IBytecodeProvider provider = (externalPath, internalPath) -> {
			if(internalPath != null) {
				String className = internalPath.substring(0, internalPath.indexOf(".class"));
				return workspace.getRawClass(className);
			}
			throw new IllegalStateException("Provider should only receive internal names."+
					"Got external name: " + externalPath);
		};
		FernFlowerAccessor decompiler = new FernFlowerAccessor(provider, collector, getOptions(), logger);
		try {
			decompiler.addWorkspace(workspace);
		} catch(IOException e) {
			e.printStackTrace();
		} catch(ReflectiveOperationException e) {
			e.printStackTrace();
		}
		decompiler.analyze();
		return decompiler.decompile(name);
	}
}
