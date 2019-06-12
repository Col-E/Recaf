package me.coley.recaf;

import me.coley.recaf.common.JavaFxTest;
import me.coley.recaf.config.impl.ConfDisplay;
import me.coley.recaf.ui.FormatFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.objectweb.asm.Opcodes.*;

/**
 * Tests for FormatFactory
 *
 * @author Matt
 */
// TODO: InvokeDynamic insns
public class FormatFactoryTest implements JavaFxTest {
	@BeforeAll
	public static void setup() {
		// Assumes the @BeforeAll's of the interfaces are called first
		ConfDisplay.instance().jumpHelp = false;
	}

	@Test
	public void testBasicInsns() {
		individual(new InsnNode(NOP), "NOP");
		individual(new IntInsnNode(BIPUSH, 10), "BIPUSH 10");
		individual(new VarInsnNode(ALOAD, 0), "ALOAD 0");
		individual(new LineNumberNode(10, new LabelNode()), "LINE 10:LABEL A");
	}

	@Test
	public void testJumpInsns() {
		// Will be shown as "LABEL A"
		LabelNode dummy = new LabelNode();
		individual(new JumpInsnNode(IFEQ, dummy), "IFEQ LABEL A");
		individual(dummy, "LABEL A");
	}

	@Test
	public void testTableSwitch() {
		// Setup
		int min = 0, max = 2;
		LabelNode dflt = new LabelNode();
		LabelNode exit = new LabelNode();
		LabelNode[] lbls = new LabelNode[]{new LabelNode(), new LabelNode(), new LabelNode()};
		MethodNode dummyMethod = new MethodNode(ACC_PUBLIC, "name", "()V", null, null);
		InsnList insns = dummyMethod.instructions = new InsnList();
		insns.add(new TableSwitchInsnNode(min, max, dflt, lbls));
		for(LabelNode lbl : lbls) {
			insns.add(lbl);
			insns.add(new InsnNode(NOP));
			insns.add(new JumpInsnNode(GOTO, exit));
		}
		insns.add(dflt);
		insns.add(new InsnNode(NOP));
		insns.add(new InsnNode(NOP));
		insns.add(new JumpInsnNode(GOTO, exit));
		insns.add(exit);
		insns.add(new InsnNode(RETURN));
		// Matching
		// - Range should be min-max
		// - Offsets should be A, B, C due to insertion order into "insns", Default then is "D"
		String[] out = FormatFactory.insnsString(asList(insns.toArray()), dummyMethod).split("\n");
		assertEquals(out[0], "0 : TABLESWITCH  range[0-2] offsets[A, B, C] default:D");
	}

	@Test
	public void testLookupSwitch() {
		LabelNode dflt = new LabelNode();
		LabelNode exit = new LabelNode();
		int[] keys = new int[]{0, 1, 2};
		LabelNode[] lbls = new LabelNode[]{new LabelNode(), new LabelNode(), new LabelNode()};
		MethodNode dummyMethod = new MethodNode(ACC_PUBLIC, "name", "()V", null, null);
		InsnList insns = dummyMethod.instructions = new InsnList();
		insns.add(new LookupSwitchInsnNode(dflt, keys, lbls));
		for(LabelNode lbl : lbls) {
			insns.add(lbl);
			insns.add(new InsnNode(NOP));
			insns.add(new JumpInsnNode(GOTO, exit));
		}
		insns.add(dflt);
		insns.add(new InsnNode(NOP));
		insns.add(new InsnNode(NOP));
		insns.add(new JumpInsnNode(GOTO, exit));
		insns.add(exit);
		insns.add(new InsnNode(RETURN));
		// Matching
		// - Keys and mappings fit a 1:1 ratio
		// - Mappings should be A, B, C due to insertion order into "insns", Default then is "D"
		String[] out = FormatFactory.insnsString(asList(insns.toArray()), dummyMethod).split("\n");
		assertEquals(out[0], "0 : LOOKUPSWITCH  mapping[0=A, 1=B, 2=C] default:D");
	}

	@Test
	public void testLdcInsns() {
		individual(new LdcInsnNode("String"), "LDC \"String\"");
		individual(new LdcInsnNode("String\nSplit"), "LDC \"String\\nSplit\"");
		individual(new LdcInsnNode(100L), "LDC 100");
		individual(new LdcInsnNode(100D), "LDC 100.0");
		individual(new LdcInsnNode(100F), "LDC 100.0");
		individual(new IincInsnNode(1, 1), "IINC $1 + 1");
		individual(new IincInsnNode(1, -1), "IINC $1 - 1");
	}

	@Test
	public void testSimplifiableInsns() {
		individual(new TypeInsnNode(NEW, "some/example/Type"),
				"NEW Type",
				"NEW some/example/Type");
		individual(new MultiANewArrayInsnNode("Ljava/lang/String;", 2),
				"MULTIANEWARRAY String[][]",
				"MULTIANEWARRAY Ljava/lang/String;[][]");
		individual(new FieldInsnNode(GETFIELD, "java/lang/System", "out", "Ljava/io/PrintStream;"),
				"GETFIELD System.out PrintStream",
				"GETFIELD java/lang/System.out Ljava/io/PrintStream;");
		individual(new MethodInsnNode(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"),
				"INVOKEVIRTUAL PrintStream.println(String)void",
				"INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V");
	}

	@Test
	public void testVariableData() {
		// Setup method
		MethodNode dummyMethod = new MethodNode(ACC_PUBLIC, "name", "(I)V", null, null);
		InsnList insns = dummyMethod.instructions = new InsnList();
		LabelNode start = new LabelNode(), end = new LabelNode();
		insns.add(start);
		insns.add(new VarInsnNode(ALOAD, 0));
		insns.add(new VarInsnNode(ILOAD, 1));
		insns.add(end);
		// Setup variables
		dummyMethod.localVariables = new ArrayList<>();
		dummyMethod.localVariables.add(new LocalVariableNode("this", "Ljava/lang/Object;", null, start, end, 0));
		dummyMethod.localVariables.add(new LocalVariableNode("param", "I", null, start, end, 1));
		// Matching
		individual(dummyMethod, insns.get(1), "1: ALOAD 0 [this:Ljava/lang/Object;]");
		individual(dummyMethod, insns.get(2), "2: ILOAD 1 [param:I]");
	}

	// ========================= UTILITY ========================= //

	private void individual(MethodNode method, AbstractInsnNode insn, String expected) {
		String actual = FormatFactory.insnNode(insn, method).getText();
		assertEquals(expected, actual);
	}

	private void individual(AbstractInsnNode insn, String expected) {
		String actual = FormatFactory.insnNode(insn, null).getText();
		assertEquals(expected, actual);
	}

	private void individual(AbstractInsnNode insn, String expectedSimple, String expectedFull) {
		ConfDisplay.instance().simplify = true;
		individual(insn, expectedSimple);
		ConfDisplay.instance().simplify = false;
		individual(insn, expectedFull);
	}
}
