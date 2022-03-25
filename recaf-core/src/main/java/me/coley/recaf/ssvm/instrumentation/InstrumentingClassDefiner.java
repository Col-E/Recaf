package me.coley.recaf.ssvm.instrumentation;

import dev.xdark.ssvm.classloading.ClassDefiner;
import dev.xdark.ssvm.classloading.ClassParseResult;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Class definer that supports class transformations.
 *
 * @author xDark
 */
public class InstrumentingClassDefiner implements ClassDefiner {

	private final List<ClassTransformer> transformers = new CopyOnWriteArrayList<>();
	private final ClassDefiner classDefiner;

	/**
	 * @param classDefiner
	 * 		Delegating class definer.
	 */
	public InstrumentingClassDefiner(ClassDefiner classDefiner) {
		this.classDefiner = classDefiner;
	}

	@Override
	public ClassParseResult parseClass(String name, byte[] classBytes, int off, int len, String source) {
		ClassParseResult result = classDefiner.parseClass(name, classBytes, off, len, source);
		if (result == null) {
			// Delegating definer failed to parse a class, return
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
