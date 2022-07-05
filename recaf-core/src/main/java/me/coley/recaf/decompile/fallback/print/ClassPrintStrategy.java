package me.coley.recaf.decompile.fallback.print;

import me.coley.recaf.decompile.fallback.model.ClassModel;

/**
 * Outlines printing a class.
 *
 * @author Matt Coley
 */
public interface ClassPrintStrategy extends PrintBase {
	/**
	 * @param model
	 * 		Class model to print.
	 *
	 * @return Printed text of model.
	 */
	String print(ClassModel model);
}
