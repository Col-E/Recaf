package me.coley.recaf.ui.context;

/**
 * Base context menu builder for declarable items such as classes, fields and methods.
 *
 * @author Matt Coley
 */
public abstract class DeclarableContextBuilder extends ContextBuilder {
	/**
	 * @param declaration
	 * 		Is {@code true} when the context is spawned from a declaration of the class/member
	 * 		rather than a reference to it.
	 *
	 * @return Builder
	 */
	public abstract DeclarableContextBuilder setDeclaration(boolean declaration);
}
