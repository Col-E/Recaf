package software.coley.recaf.cdi;

/**
 * Initialization stage for {@link EagerInitialization#value()}.
 *
 * @author Matt Coley
 * @see EagerInitialization
 */
public enum InitializationStage {
	/**
	 * Occurs as soon as possible.
	 */
	IMMEDIATE,
	/**
	 * Occurs after the UI is initialized.
	 */
	AFTER_UI_INIT
}
