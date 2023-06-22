package software.coley.recaf.ui.control.richtext.linegraphics;

/**
 * Base implementation of {@link LineGraphicFactory}.
 *
 * @author Matt Coley
 */
public abstract class AbstractLineGraphicFactory implements LineGraphicFactory {
	private final int priority;

	/**
	 * @param priority
	 * 		Priority dictating the order of graphics displayed in {@link RootLineGraphicFactory}.
	 * 		See {@link LineGraphicFactory} for constants.
	 */
	protected AbstractLineGraphicFactory(int priority) {
		this.priority = priority;
	}

	@Override
	public int priority() {
		return priority;
	}
}
