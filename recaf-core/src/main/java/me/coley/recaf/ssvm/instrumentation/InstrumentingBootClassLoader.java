package me.coley.recaf.ssvm.instrumentation;

import dev.xdark.ssvm.classloading.BootClassLoader;
import dev.xdark.ssvm.classloading.ClassParseResult;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Boot class loader that supports class transformations.
 *
 * @author xDark
 */
public class InstrumentingBootClassLoader implements BootClassLoader {
	private final List<ClassTransformer> transformers = new CopyOnWriteArrayList<>();
	private final BootClassLoader bootClassLoader;

	/**
	 * @param bootClassLoader
	 * 		Delegating boot class loader.
	 */
	public InstrumentingBootClassLoader(BootClassLoader bootClassLoader) {
		this.bootClassLoader = bootClassLoader;
	}

	@Override
	public ClassParseResult findBootClass(String name) {
		ClassParseResult result = bootClassLoader.findBootClass(name);
		if (result == null) {
			// Delegating boot class loader did not find a class, return
			return null;
		}
		for (ClassTransformer transformer : transformers) {
			if ((result = transformer.transform(result)) == null) {
				return null;
			}
		}
		return result;
	}

	/**
	 * Registers transformer.
	 *
	 * @param transformer
	 * 		Transformer to register.
	 */
	public void addTransformer(ClassTransformer transformer) {
		transformers.add(transformer);
	}

	/**
	 * Unregisters transformer.
	 *
	 * @param transformer
	 * 		Transformer to unregister.
	 */
	public void removeTransformer(ClassTransformer transformer) {
		transformers.remove(transformer);
	}
}
