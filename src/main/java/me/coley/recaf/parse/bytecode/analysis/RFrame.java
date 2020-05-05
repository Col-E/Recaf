package me.coley.recaf.parse.bytecode.analysis;

import org.objectweb.asm.tree.analysis.Frame;

import java.util.*;

/**
 * Frame for {@link RValue} content.
 *
 * @author Matt
 */
public class RFrame extends Frame<RValue> {
	private final Set<Integer> reservedSlots = new HashSet<>();

	/**
	 * New frame of size.
	 *
	 * @param numLocals
	 * 		Maximum number of local variables of the frame.
	 * @param numStack
	 * 		Maximum stack size of the frame.
	 */
	public RFrame(int numLocals, int numStack) {
		super(numLocals, numStack);
	}

	/**
	 * Copy frame.
	 *
	 * @param frame
	 * 		Another frame..
	 */
	public RFrame(final RFrame frame) {
		super(frame);
	}

	@Override
	public void setLocal(int index, RValue value) {
		if (!value.isUninitialized()) {
			// Check against reserved slots used by double and long locals
			if(reservedSlots.contains(index))
				throw new IllegalStateException("Cannot set local[" + index + "] " +
						"since it is reserved by a double/long (which reserves two slots)");
			if(value.getValue() instanceof Double || value.getValue() instanceof Long)
				reservedSlots.add(index + 1);
		}
		// Update local
		super.setLocal(index, value);
	}
}
