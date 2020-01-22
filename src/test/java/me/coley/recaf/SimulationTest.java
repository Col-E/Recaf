package me.coley.recaf;

import me.coley.recaf.simulation.ExecutionContext;
import me.coley.recaf.simulation.SimulationException;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import static org.objectweb.asm.Opcodes.*;
import static org.junit.jupiter.api.Assertions.*;

public class SimulationTest {

	@Test
	public void testAdd() throws SimulationException {
		InsnList list = new InsnList();
		list.add(new InsnNode(ICONST_2));
		list.add(new InsnNode(ICONST_3));
		list.add(new InsnNode(IADD));
		list.add(new InsnNode(IRETURN));
		ExecutionContext<Integer> ctx = new ExecutionContext<>(null, 2, 0, list, null);
		assertEquals(5, ctx.run());
	}

	@Test
	public void testMultiply() throws SimulationException {
		InsnList list = new InsnList();
		list.add(new InsnNode(ICONST_2));
		list.add(new InsnNode(ICONST_2));
		list.add(new InsnNode(IMUL));
		list.add(new InsnNode(IRETURN));
		ExecutionContext<Integer> ctx = new ExecutionContext<>(null, 2, 0, list, null);
		assertEquals(4, ctx.run());
	}


	@Test
	public void testXor() throws SimulationException {
		InsnList list = new InsnList();
		list.add(new InsnNode(LCONST_1));
		list.add(new InsnNode(LCONST_1));
		list.add(new InsnNode(LXOR));
		list.add(new InsnNode(LRETURN));
		ExecutionContext<Long> ctx = new ExecutionContext<>(null, 4, 0, list, null);
		assertEquals( 0L, ctx.run());
	}
}
