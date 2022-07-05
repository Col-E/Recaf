package me.coley.recaf.decompile.fallback.print;

import me.coley.recaf.decompile.fallback.model.ClassModel;
import me.coley.recaf.decompile.fallback.model.MethodModel;

/**
 * Outlines printing a method.
 *
 * @author Matt Coley
 */
public interface MethodPrintStrategy extends PrintBase {
	/**
	 * @param parent
	 * 		Class model containing the method.
	 * @param model
	 * 		Method model to print.
	 *
	 * @return Printed text of model.
	 */
	String print(ClassModel parent, MethodModel model);
}
