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

	/**
	 * Action to run for opening an assembler of the declared class/member.
	 */
	public abstract void assemble();

	/**
	 * Action to run for opening the definition of the declared class/member.
	 */
	public abstract void openDefinition();

	/**
	 * Action to run for renaming the declared class/member.
	 */
	public abstract void rename();

	/**
	 * Action to run for removing the declared class/member.
	 */
	public abstract void delete();

	/**
	 * Action to run for copying the declared class/member.
	 */
	public abstract void copy();

	/**
	 * Action to run for searching for references to the declared class/member.
	 */
	public abstract void search();
}
