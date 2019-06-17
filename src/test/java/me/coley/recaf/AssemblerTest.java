package me.coley.recaf;

import me.coley.recaf.bytecode.AccessFlag;
import me.coley.recaf.bytecode.InsnUtil;
import me.coley.recaf.bytecode.insn.NamedLabelNode;
import org.objectweb.asm.tree.*;
import me.coley.recaf.parse.assembly.Assembly;
import me.coley.recaf.parse.assembly.exception.ExceptionWrapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * Tests for Assembler
 *
 * @author Matt
 */
// TODO: Tests for compiling:
// - TableSwitchInsnNode
// - LookupSwitchInsnNode
// - LineNumberNode
// TODO: Tests for verifying bad syntax doesn't compile
public class AssemblerTest {
	private final Assembly asm = new Assembly();

	@Test
	public void testIndividualInsns() {
		asm.setMethodDeclaration(ACC_PUBLIC, "name", "()V");
		asm.setDoVerify(false);
		asm.setDoGenerateLocals(false);
		checkInsnMatch("ALOAD 0", new VarInsnNode(ALOAD, 0));
		checkInsnMatch("ALOAD this", new VarInsnNode(ALOAD, 0));
		checkInsnMatch("BIPUSH 10", new IntInsnNode(BIPUSH, 10));
		checkInsnMatch("NEW java/io/InputStream", new TypeInsnNode(NEW, "java/io/InputStream"));
		checkInsnMatch("LDC \"String\"", new LdcInsnNode("String"));
		checkInsnMatch("LDC 10L", new LdcInsnNode(10L));
		checkInsnMatch("LDC 10D", new LdcInsnNode(10D));
		checkInsnMatch("LDC 10F", new LdcInsnNode(10F));
		checkInsnMatch("IINC 1 + 1", new IincInsnNode(1, 1));
		checkInsnMatch("IINC 1 - 1", new IincInsnNode(1, -1));
		checkInsnMatch("MULTIANEWARRAY java/lang/String 2",
				new MultiANewArrayInsnNode("java/lang/String", 2));
		checkInsnMatch("GETFIELD java/lang/System.out Ljava/io/PrintStream;",
				new FieldInsnNode(GETFIELD, "java/lang/System", "out", "Ljava/io/PrintStream;"));
		checkInsnMatch("INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V",
				new MethodInsnNode(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"));
	}

	@Test
	public void testVerifyExpectedVarTypes() {
		// Setting up some example scenario, a class for a painting.
		// The method will draw a color at a location with some given amount of blur.
		/*
		public void draw(int x, int y, double blur, Color color) {
			Pixel pixel = getPixel(x, y);
			pixel.setColor(color, blur);
		}
		 */
		asm.setHostType("example/Painting");
		asm.setMethodDeclaration(ACC_PUBLIC, "draw", "(IIDLexample/Color;)V");
		asm.setDoVerify(true);
		asm.setDoGenerateLocals(true);
		String[] lines = new String[] {
				"ALOAD this",
				"ILOAD p1x",
				"ILOAD p2y",
				"INVOKESPECIAL example/Painting.getPixel(II)Lexample/Pixel;",
				"ASTORE pixel",
				"ALOAD pixel",
				"ALOAD p4color",
				"DLOAD p3blur",
				"INVOKEVIRTUAL example/Painting.setColor(Lexample/Color;D)V",
				"RETURN"
		};
		assertTrue(asm.parseInstructions(lines));
		// Test locals
		LocalVariableNode lThis = InsnUtil.getLocal(asm.getMethod(), 0);
		assertNotNull(lThis);
		assertEquals("this", lThis.name);
		assertEquals("Lexample/Painting;", lThis.desc);
		LocalVariableNode lX = InsnUtil.getLocal(asm.getMethod(), 1);
		assertNotNull(lX);
		assertEquals("x", lX.name);
		assertEquals("I", lX.desc);
		LocalVariableNode lY = InsnUtil.getLocal(asm.getMethod(), 2);
		assertNotNull(lY);
		assertEquals("y", lY.name);
		assertEquals("I", lY.desc);
		LocalVariableNode lBlur = InsnUtil.getLocal(asm.getMethod(), 3);
		assertNotNull(lBlur);
		assertEquals("blur", lBlur.name);
		assertEquals("D", lBlur.desc);
		// Index is 5, not 4 because "blur" which is a double, takes 2 local variable spaces
		LocalVariableNode lColor = InsnUtil.getLocal(asm.getMethod(), 5);
		assertNotNull(lColor);
		assertEquals("color", lColor.name);
		assertEquals("Lexample/Color;", lColor.desc);
		// "pixel" should be 6 because it is the next open space after 5
		// Method-locals should start indexing just after the highest parameter value.
		LocalVariableNode lPixel = InsnUtil.getLocal(asm.getMethod(), 6);
		assertNotNull(lPixel);
		assertEquals("pixel", lPixel.name);
		assertEquals("Lexample/Pixel;", lPixel.desc);
	}

	@Test
	public void testDebugLabelsReplaced() {
		asm.setMethodDeclaration(ACC_PUBLIC, "name", "(Z)V");
		asm.setDoGenerateLocals(false);
		asm.setDoVerify(false);
		String[] lines = new String[] {
				"ILOAD p1Bool",
				"IFEQ SomeLabel",
				"NOP",
				"LABEL SomeLabel",
				"RETURN"
		};
		assertTrue(asm.parseInstructions(lines));
		JumpInsnNode jin = (JumpInsnNode) asm.getMethod().instructions.get(1);
		LabelNode lbl = (LabelNode) asm.getMethod().instructions.get(3);
		// Named labels should be replaced
		assertFalse(jin.label instanceof NamedLabelNode);
		assertFalse(lbl instanceof NamedLabelNode);
	}

	@Test
	public void testVerifyPopNoStack() {
		asm.setMethodDeclaration(ACC_PUBLIC, "name", "()V");
		asm.setDoVerify(true);
		// One value on the stack, but two values are popped off.
		String[] lines = new String[] {
				"ICONST_0",
				"POP",
				"POP",
				"RETURN"
		};
		// Parse should fail due to verification on 3rd line
		assertFalse(asm.parseInstructions(lines));
		ExceptionWrapper wrapper = asm.getExceptions().get(0);
		assertEquals(wrapper.line, 3);
		assertTrue(wrapper.exception.toString().contains("Cannot pop operand off an empty stack"));
	}

	@Test
	public void testVerifyNoReturn() {
		asm.setMethodDeclaration(ACC_PUBLIC, "name", "()V");
		asm.setDoVerify(true);
		String[][] cases = new String[][] {
				// Do-nothing void methods still require a RETURN at the end.
				new String[] {
						"NOP"
				},
				// The jump can skip past the RETURN
				new String[] {
						"ICONST_0",
						"IFEQ after",
						"LABEL before",
						"RETURN",
						"LABEL after"
				}
		};
		for (String[] lines : cases) {
			assertFalse(asm.parseInstructions(lines));
			ExceptionWrapper wrapper = asm.getExceptions().get(0);
			assertEquals(wrapper.line, -1);
			assertTrue(wrapper.exception.toString().contains("fall off"));
		}
	}

	// ========================= UTILITY ========================= //

	private <T extends AbstractInsnNode> T getInsn(String line) {
		asm.parseInstructions(new String[] {line});
		if (!asm.getExceptions().isEmpty()) {
			asm.getExceptions().forEach(
					wrap -> wrap.printStackTrace());
			fail("Parse failure");
		}
		// Should only be one instruction
		assertEquals(1, asm.getMethod().instructions.size());
		return (T) asm.getMethod().instructions.get(0);
	}

	private void checkInsnMatch(String line, AbstractInsnNode expected) {
		AbstractInsnNode insn = getInsn(line);
		assertEquals(expected.getOpcode(), insn.getOpcode());
		reflectEquals(expected, insn);
	}

	private void reflectEquals(Object expected, Object actual) {
		assertNotNull(expected);
		assertNotNull(actual);
		Class<?> ce = expected.getClass();
		Class<?> ca = actual.getClass();
		assertEquals(ce, ca);
		for (int i = 0; i < ce.getDeclaredFields().length; i++) {
			Field f1 = ce.getDeclaredFields()[i];
			// Skip static, final, and non-publics
			if (AccessFlag.isStatic(f1.getModifiers()) ||
					AccessFlag.isFinal(f1.getModifiers()) ||
					!AccessFlag.isPublic(f1.getModifiers())) {
				continue;
			}
			Field f2 = ce.getDeclaredFields()[i];
			try {
				f1.setAccessible(true);
				f2.setAccessible(true);
				assertEquals(f1.get(expected), f2.get(actual), "Mismatch: " + f1.toString());
			} catch(ReflectiveOperationException e) {
				fail(e);
			}
		}
	}
}
