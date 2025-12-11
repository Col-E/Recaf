package software.coley.recaf.behavior;

/**
 * Default keys for {@link PrioritySortable} implementations.
 * <p/>
 * Searching for usages of these keys will show what listeners/classes fire in which order.
 *
 * @author Matt Coley
 */
public final class PriorityKeys {
	public static final int EARLIEST = -1000;
	public static final int EARLIER = -100;
	public static final int EARLY = -10;
	public static final int DEFAULT = 0;
	public static final int LATE = 10;
	public static final int LATER = 100;
	public static final int LATEST = 1000;

	private PriorityKeys() {}
}
