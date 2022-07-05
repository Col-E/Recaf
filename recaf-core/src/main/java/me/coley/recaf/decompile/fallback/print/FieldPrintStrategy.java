package me.coley.recaf.decompile.fallback.print;

import me.coley.recaf.decompile.fallback.model.ClassModel;
import me.coley.recaf.decompile.fallback.model.FieldModel;

/**
 * Outlines printing a field.
 *
 * @author Matt Coley
 */
public interface FieldPrintStrategy extends PrintBase {
	/**
	 * @param parent
	 * 		Class model containing the field.
	 * @param model
	 * 		Field model to print.
	 *
	 * @return Printed text of model.
	 */
	String print(ClassModel parent, FieldModel model);
}
