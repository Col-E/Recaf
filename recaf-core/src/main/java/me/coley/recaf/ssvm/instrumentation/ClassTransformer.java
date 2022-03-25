package me.coley.recaf.ssvm.instrumentation;

import dev.xdark.ssvm.classloading.ClassParseResult;

/**
 * Instrumentation-like API for class transformation.
 *
 * @author xDark
 */
@FunctionalInterface
public interface ClassTransformer {
	/**
	 * Performs class transformation.
	 *
	 * @param result
	 * 		Class to transformed.
	 *
	 * @return transformed class.
	 */
	ClassParseResult transform(ClassParseResult result);
}
