package me.coley.recaf;

import me.coley.recaf.parse.assembly.LineParseException;
import me.coley.recaf.parse.assembly.visitors.AssemblyVisitor;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.InsnList;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * Tests for the assembly.
 *
 * @author Matt
 */
public class AssemblyTest extends Base {
	@Test
	public void testInsn() {
		try {
			AssemblyVisitor visitor = new AssemblyVisitor();
			visitor.visit("ACONST_NULL\nARETURN");
			// Two simple InsnNode instructions
			InsnList insns = visitor.getInsnList();
			assertEquals(2, insns.size());
			assertEquals(ACONST_NULL, insns.get(0).getOpcode());
			assertEquals(ARETURN, insns.get(1).getOpcode());
		} catch(LineParseException ex) {
			fail(ex);
		}
	}

	@Test
	public void testBadInsn() {
		AssemblyVisitor visitor = new AssemblyVisitor();
		// AssemblyVisitor won't be able to resolve this fake opcode, thus throws a line parser err
		assertThrows(LineParseException.class, () -> visitor.visit("ACONST_NOT_REAL"));
	}

	@Test
	public void testInsnSuggest() {
		try {
			AssemblyVisitor visitor = new AssemblyVisitor();
			List<String> suggestions = visitor.suggest("ACONST_");
			// Only opcode to match should be ACONST_NULL
			assertEquals(1, suggestions.size());
			assertEquals("ACONST_NULL", suggestions.get(0));
		} catch(LineParseException ex) {
			fail(ex);
		}
	}

	@Test
	public void testBadInsnSuggest() {
		try {
			AssemblyVisitor visitor = new AssemblyVisitor();
			List<String> suggestions = visitor.suggest("ACONST_NOT_REAL");
			// Suggest doesn't care the opcode is bad.
			// We'll just return nothing
			assertEquals(0, suggestions.size());
		} catch(LineParseException ex) {
			fail(ex);
		}
	}
}
