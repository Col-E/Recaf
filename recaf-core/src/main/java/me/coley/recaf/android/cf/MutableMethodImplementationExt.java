package me.coley.recaf.android.cf;

import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.debug.DebugItem;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * There is already a {@link MutableMethodImplementation} but it does not copy over debug information.
 * This provides a mutable copy of those debug items as well.
 *
 * @author Matt Coley
 */
public class MutableMethodImplementationExt extends MutableMethodImplementation {
	private final List<DebugItem> debugItems;

	/**
	 * @param original
	 * 		Instance to copy.
	 */
	public MutableMethodImplementationExt(MethodImplementation original) {
		super(original);
		debugItems = copyItems(original.getDebugItems());
	}

	@Nonnull
	@Override
	public List<DebugItem> getDebugItems() {
		return debugItems;
	}

	private static List<DebugItem> copyItems(Iterable<? extends DebugItem> debugItems) {
		List<DebugItem> list = new ArrayList<>();
		for (DebugItem item : debugItems) {
			list.add(item);
		}
		return list;
	}
}
