package me.coley.recaf.presentation;

/**
 * Presentation UI type.
 *
 * @author Matt Coley
 */
public enum PresentationType {
	GUI("me.coley.recaf.presentation.GuiPresentation"),
	HEADLESS("me.coley.recaf.presentation.HeadlessPresentation"),
	NONE("me.coley.recaf.presentation.EmptyPresentation");

	private final String presentationClassName;

	PresentationType(String presentationClassName) {
		this.presentationClassName = presentationClassName;
	}

	/**
	 * @return New presentation instance from type.
	 *
	 * @throws ReflectiveOperationException
	 * 		When the implementation instance could not be opened.
	 */
	public Presentation createInstance() throws ReflectiveOperationException {
		return (Presentation) Class.forName(presentationClassName).getConstructor().newInstance();
	}
}
