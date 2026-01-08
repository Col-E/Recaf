package software.coley.recaf.ui.control.richtext.inheritance;

import jakarta.annotation.Nonnull;
import software.coley.collections.Unchecked;
import software.coley.recaf.ui.control.richtext.AbstractLineItemTracking;
import software.coley.recaf.ui.control.richtext.Editor;

/**
 * Tracking for method inheritance icons to display in an {@link Editor}.
 *
 * @author Matt Coley
 */
public class InheritanceTracking extends AbstractLineItemTracking<Inheritance, InheritanceInvalidationListener> {
	/** Key for {@link Editor#getComponent(java.lang.String)}. */
	public static final String COMPONENT_KEY = "inheritance-tracking";

	@Override
	protected void notifyListeners(@Nonnull String failureMessage) {
		Unchecked.checkedForEach(listeners, InheritanceInvalidationListener::onInheritanceInvalidation,
				(listener, t) -> logger.error(failureMessage, t));
	}

	@Override
	protected int getLine(@Nonnull Inheritance item) {
		return item.line();
	}

	@Override
	protected Inheritance withLine(@Nonnull Inheritance item, int newLine) {
		return item.withLine(newLine);
	}
}
