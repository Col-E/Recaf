package me.coley.recaf.parse.bytecode;

import org.objectweb.asm.tree.analysis.Frame;

import java.util.*;

public class RFrame extends Frame<RValue> {
	private final Set<Integer> reservedSlots = new HashSet<>();

	public RFrame(int numLocals, int numStack) {
		super(numLocals, numStack);
	}

	public RFrame(final RFrame frame) {
		super(frame);
	}

	@Override
	public void setLocal(int index, RValue value) {
		// Check against reserved slots used by double and long locals
		if (reservedSlots.contains(index))
			throw new IllegalStateException("Cannot set local[" + index +"] " +
					"since it is reserved by a double/long (which reserves two slots)");
		if (value.getValue() instanceof Double || value.getValue() instanceof Long)
			reservedSlots.add(index + 1);
		// Update local
		super.setLocal(index, value);
	}
}
