package software.coley.recaf.util;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Type.*;
import static software.coley.recaf.util.AsmInsnUtil.*;

/**
 * Tests for {@link AsmInsnUtil}.
 */
class AsmInsnUtilTest {
	@Test
	void testOpcodeToTag() {
		assertEquals(H_INVOKESPECIAL, opcodeToTag(INVOKESPECIAL));
		assertEquals(H_INVOKEINTERFACE, opcodeToTag(INVOKEINTERFACE));
		assertEquals(H_INVOKEVIRTUAL, opcodeToTag(INVOKEVIRTUAL));
		assertEquals(H_INVOKESTATIC, opcodeToTag(INVOKESTATIC));
		assertEquals(H_GETFIELD, opcodeToTag(GETFIELD));
		assertEquals(H_GETSTATIC, opcodeToTag(GETSTATIC));
		assertEquals(H_PUTFIELD, opcodeToTag(PUTFIELD));
		assertEquals(H_PUTSTATIC, opcodeToTag(PUTSTATIC));

		// Other instructions don't have handle tags, so this is not allowed.
		assertThrows(IllegalStateException.class, () -> opcodeToTag(ICONST_0));
	}

	@Test
	void testTagToOpcode() {
		assertEquals(INVOKESPECIAL, tagToOpcode(H_INVOKESPECIAL));
		assertEquals(INVOKEINTERFACE, tagToOpcode(H_INVOKEINTERFACE));
		assertEquals(INVOKEVIRTUAL, tagToOpcode(H_INVOKEVIRTUAL));
		assertEquals(INVOKESTATIC, tagToOpcode(H_INVOKESTATIC));
		assertEquals(GETFIELD, tagToOpcode(H_GETFIELD));
		assertEquals(GETSTATIC, tagToOpcode(H_GETSTATIC));
		assertEquals(PUTFIELD, tagToOpcode(H_PUTFIELD));
		assertEquals(PUTSTATIC, tagToOpcode(H_PUTSTATIC));

		// Unknown tag values do not have respective opcodes, so this is not allowed.
		assertThrows(IllegalStateException.class, () -> tagToOpcode(-1));
	}

	@Test
	void testIndexOf() {
		// Items in the list should match the list index.
		InsnList list = new InsnList();
		for (int i = 0; i < 25; i++)
			list.add(new InsnNode(NOP));
		for (int i = 0; i < 25; i++)
			assertEquals(i, indexOf(list.get(i)));

		// Items not in any list have no "previous" instruction, so they
		// always appear as if they are first.
		assertEquals(0, indexOf(new InsnNode(NOP)));
	}

	@Test
	void testGetTypeForVarInsn() {
		// int
		assertEquals(INT_TYPE, getTypeForVarInsn(new VarInsnNode(ILOAD, 0)));
		assertEquals(INT_TYPE, getTypeForVarInsn(new VarInsnNode(ISTORE, 0)));

		// float
		assertEquals(FLOAT_TYPE, getTypeForVarInsn(new VarInsnNode(FLOAD, 0)));
		assertEquals(FLOAT_TYPE, getTypeForVarInsn(new VarInsnNode(FSTORE, 0)));

		// long
		assertEquals(LONG_TYPE, getTypeForVarInsn(new VarInsnNode(LLOAD, 0)));
		assertEquals(LONG_TYPE, getTypeForVarInsn(new VarInsnNode(LSTORE, 0)));

		// double
		assertEquals(DOUBLE_TYPE, getTypeForVarInsn(new VarInsnNode(DLOAD, 0)));
		assertEquals(DOUBLE_TYPE, getTypeForVarInsn(new VarInsnNode(DSTORE, 0)));

		// object/array
		assertEquals(Types.OBJECT_TYPE, getTypeForVarInsn(new VarInsnNode(ALOAD, 0)));
		assertEquals(Types.OBJECT_TYPE, getTypeForVarInsn(new VarInsnNode(ASTORE, 0)));
	}

	@Test
	void testIsVarStore() {
		for (int i = 0; i < 255; i++) {
			if (i == ISTORE || i == FSTORE || i == LSTORE || i == DSTORE || i == ASTORE) {
				assertTrue(isVarStore(i));
			} else {
				assertFalse(isVarStore(i));
			}
		}
	}

	@Test
	void testIsVarLoad() {
		for (int i = 0; i < 255; i++) {
			if (i == ILOAD || i == FLOAD || i == LLOAD || i == DLOAD || i == ALOAD) {
				assertTrue(isVarLoad(i));
			} else {
				assertFalse(isVarLoad(i));
			}
		}
	}

	@Test
	void testCreateVarLoad() {
		Type[] types = new Type[]{BOOLEAN_TYPE, CHAR_TYPE, SHORT_TYPE, INT_TYPE,
				FLOAT_TYPE, LONG_TYPE, DOUBLE_TYPE, Types.OBJECT_TYPE};
		for (Type type : types) {
			Type loadType = type;
			if (loadType.getSort() < INT)
				loadType = INT_TYPE;
			VarInsnNode varLoad = createVarLoad(0, type);
			assertTrue(isVarLoad(varLoad.getOpcode()));
			assertEquals(loadType, getTypeForVarInsn(varLoad));
		}
	}

	@Test
	void testCreateVarStore() {
		Type[] types = new Type[]{BOOLEAN_TYPE, CHAR_TYPE, SHORT_TYPE, INT_TYPE,
				FLOAT_TYPE, LONG_TYPE, DOUBLE_TYPE, Types.OBJECT_TYPE};
		for (Type type : types) {
			Type storeType = type;
			if (storeType.getSort() < INT)
				storeType = INT_TYPE;
			VarInsnNode varStore = createVarStore(0, type);
			assertTrue(isVarStore(varStore.getOpcode()));
			assertEquals(storeType, getTypeForVarInsn(varStore));
		}
	}

	@Test
	void testFixMissingVariableLabels() {
		// Create method with just "return parameter[0]"
		MethodNode method = new MethodNode(ACC_STATIC, "foo", "(I)I", null, null);
		method.instructions.add(new VarInsnNode(ILOAD, 0));
		method.instructions.add(new InsnNode(IRETURN));

		// Add variable to the method with labels that do not exist in the method.
		LabelNode varStart = new LabelNode();
		LabelNode varEnd = new LabelNode();
		LocalVariableNode variable = new LocalVariableNode("param", "I", null, varStart, varEnd, 0);
		method.localVariables.add(variable);

		// They won't exist by default.
		assertFalse(method.instructions.contains(varStart));
		assertFalse(method.instructions.contains(varEnd));

		// We "fix" the method labels.
		fixMissingVariableLabels(method);

		// The labels should be re-assigned.
		assertNotSame(varStart, variable.start);
		assertNotSame(varEnd, variable.end);

		// The new labels should be in the method.
		assertSame(variable.start, method.instructions.getFirst());
		assertSame(variable.end, method.instructions.getLast());
	}

	@Test
	void testFixMissingVariableLabelsNoVars() {
		// Create method with just "return parameter[0]"
		MethodNode method = new MethodNode(ACC_STATIC, "foo", "(I)I", null, null);
		method.instructions.add(new VarInsnNode(ILOAD, 0));
		method.instructions.add(new InsnNode(IRETURN));

		// We "fix" the method labels, even though there are no variables.
		// This should do nothing.
		fixMissingVariableLabels(method);

		// No changes should be made. IE, no added labels.
		assertEquals(2, method.instructions.size());
	}

	@Test
	void testFixMissingVariableLabelsNoCode() {
		// Create method with just "return parameter[0]"
		MethodNode method = new MethodNode(ACC_STATIC | ACC_ABSTRACT, "foo", "(I)I", null, null);

		// We "fix" the method labels, even though there are no variables.
		// This should do nothing.
		fixMissingVariableLabels(method);

		// No changes should be made. IE, no added labels.
		assertEquals(0, method.instructions.size());
	}

	@Test
	void testIsConstValue() {
		int[] constValues = {
				ACONST_NULL,
				ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5,
				LCONST_0, LCONST_1,
				FCONST_0, FCONST_1, FCONST_2,
				DCONST_0, DCONST_1,
				BIPUSH, SIPUSH,
				LDC,
		};
		for (int constOp : constValues)
			assertTrue(isConstValue(constOp));

		// [ACONST_NULL, ... , LDC] is a range of instructions that all push constant values.
		// As of Java 25 there are no other instructions that push "constant" values outside this range.
		assertFalse(isConstValue(NOP));
		for (int i = ILOAD; i < 200; i++)
			assertFalse(isConstValue(i));
	}

	@Test
	void testIsConstIntValue() {
		// Const-int values
		assertTrue(isConstIntValue(new InsnNode(ICONST_M1)));
		assertTrue(isConstIntValue(new InsnNode(ICONST_0)));
		assertTrue(isConstIntValue(new InsnNode(ICONST_1)));
		assertTrue(isConstIntValue(new InsnNode(ICONST_2)));
		assertTrue(isConstIntValue(new InsnNode(ICONST_3)));
		assertTrue(isConstIntValue(new InsnNode(ICONST_4)));
		assertTrue(isConstIntValue(new InsnNode(ICONST_5)));
		assertTrue(isConstIntValue(new IntInsnNode(SIPUSH, 0)));
		assertTrue(isConstIntValue(new IntInsnNode(BIPUSH, 0)));
		assertFalse(isConstIntValue(new LdcInsnNode(100000)));

		// Non const-int values
		assertFalse(isConstIntValue(new InsnNode(ACONST_NULL)));
		assertFalse(isConstIntValue(new InsnNode(FCONST_0)));
		assertFalse(isConstIntValue(new InsnNode(FCONST_1)));
		assertFalse(isConstIntValue(new InsnNode(FCONST_2)));
		assertFalse(isConstIntValue(new InsnNode(DCONST_0)));
		assertFalse(isConstIntValue(new InsnNode(DCONST_1)));
		assertFalse(isConstIntValue(new LdcInsnNode("")));
		assertFalse(isConstIntValue(new LdcInsnNode(Math.PI)));
		assertFalse(isConstIntValue(new LdcInsnNode(Float.MAX_VALUE)));
		assertFalse(isConstIntValue(new LdcInsnNode(Long.MAX_VALUE)));
		assertFalse(isConstIntValue(new LdcInsnNode(Double.MAX_VALUE)));
		assertFalse(isConstIntValue(new LabelNode()));
		assertFalse(isConstIntValue(new VarInsnNode(ILOAD, 0)));
	}

	@Test
	void testGetDefaultValue() {
		// Types that widen to int
		Type[] intTypes = new Type[]{BOOLEAN_TYPE, CHAR_TYPE, SHORT_TYPE, INT_TYPE};
		for (Type type : intTypes)
			assertEquals(ICONST_0, getDefaultValue(type).getOpcode());

		// Other primitive types
		assertEquals(FCONST_0, getDefaultValue(FLOAT_TYPE).getOpcode());
		assertEquals(LCONST_0, getDefaultValue(LONG_TYPE).getOpcode());
		assertEquals(DCONST_0, getDefaultValue(DOUBLE_TYPE).getOpcode());

		// Types that widen to object
		assertEquals(ACONST_NULL, getDefaultValue(Types.OBJECT_TYPE).getOpcode());
		assertEquals(ACONST_NULL, getDefaultValue(Types.STRING_TYPE).getOpcode());
		assertEquals(ACONST_NULL, getDefaultValue(Types.ARRAY_1D_BYTE).getOpcode());
	}

	@Test
	void testIntToInsn() {
		// Special ICONST_X cases
		assertEquals(ICONST_M1, intToInsn(-1).getOpcode());
		assertEquals(ICONST_0, intToInsn(0).getOpcode());
		assertEquals(ICONST_1, intToInsn(1).getOpcode());
		assertEquals(ICONST_2, intToInsn(2).getOpcode());
		assertEquals(ICONST_3, intToInsn(3).getOpcode());
		assertEquals(ICONST_4, intToInsn(4).getOpcode());
		assertEquals(ICONST_5, intToInsn(5).getOpcode());

		// Byte range
		assertEquals(BIPUSH, intToInsn(-2).getOpcode());
		assertEquals(BIPUSH, intToInsn(6).getOpcode());
		assertEquals(BIPUSH, intToInsn(Byte.MIN_VALUE).getOpcode());
		assertEquals(BIPUSH, intToInsn(Byte.MAX_VALUE).getOpcode());

		// Short range
		assertEquals(SIPUSH, intToInsn(Short.MIN_VALUE).getOpcode());
		assertEquals(SIPUSH, intToInsn(Short.MAX_VALUE).getOpcode());
		assertEquals(SIPUSH, intToInsn(Short.MIN_VALUE / 2).getOpcode());
		assertEquals(SIPUSH, intToInsn(Short.MAX_VALUE / 2).getOpcode());

		// Int range
		assertEquals(LDC, intToInsn(Integer.MIN_VALUE).getOpcode());
		assertEquals(LDC, intToInsn(Integer.MAX_VALUE).getOpcode());
		assertEquals(LDC, intToInsn(Integer.MIN_VALUE / 2).getOpcode());
		assertEquals(LDC, intToInsn(Integer.MAX_VALUE / 2).getOpcode());
	}

	@Test
	void testFloatToInsn() {
		// Special FCONST_X cases
		assertEquals(FCONST_0, floatToInsn(0).getOpcode());
		assertEquals(FCONST_1, floatToInsn(1).getOpcode());
		assertEquals(FCONST_2, floatToInsn(2).getOpcode());
		assertEquals(FCONST_0, floatToInsn(0.0F).getOpcode());
		assertEquals(FCONST_1, floatToInsn(1.0F).getOpcode());
		assertEquals(FCONST_2, floatToInsn(2.0F).getOpcode());

		// Float range
		assertEquals(LDC, floatToInsn(5F).getOpcode());
		assertEquals(LDC, floatToInsn(Float.MIN_VALUE).getOpcode());
		assertEquals(LDC, floatToInsn(Float.MAX_VALUE).getOpcode());
	}

	@Test
	void testDoubleToInsn() {
		// Special DCONST_X cases
		assertEquals(DCONST_0, doubleToInsn(0).getOpcode());
		assertEquals(DCONST_1, doubleToInsn(1).getOpcode());
		assertEquals(DCONST_0, doubleToInsn(0.0F).getOpcode());
		assertEquals(DCONST_1, doubleToInsn(1.0F).getOpcode());
		assertEquals(DCONST_0, doubleToInsn(0.0D).getOpcode());
		assertEquals(DCONST_1, doubleToInsn(1.0D).getOpcode());

		// Double range
		assertEquals(LDC, doubleToInsn(5.5F).getOpcode());
		assertEquals(LDC, doubleToInsn(5.5D).getOpcode());
		assertEquals(LDC, doubleToInsn(Float.MIN_VALUE).getOpcode());
		assertEquals(LDC, doubleToInsn(Float.MAX_VALUE).getOpcode());
		assertEquals(LDC, doubleToInsn(Double.MIN_VALUE).getOpcode());
		assertEquals(LDC, doubleToInsn(Double.MAX_VALUE).getOpcode());
	}

	@Test
	void testLongToInsn() {
		// Special LCONST_X cases
		assertEquals(LCONST_0, longToInsn(0).getOpcode());
		assertEquals(LCONST_1, longToInsn(1).getOpcode());
		assertEquals(LCONST_0, longToInsn(0L).getOpcode());
		assertEquals(LCONST_1, longToInsn(1L).getOpcode());

		// Double range
		assertEquals(LDC, longToInsn(100L).getOpcode());
		assertEquals(LDC, longToInsn(100000000L).getOpcode());
		assertEquals(LDC, longToInsn(Long.MIN_VALUE).getOpcode());
		assertEquals(LDC, longToInsn(Long.MAX_VALUE).getOpcode());
		assertEquals(LDC, longToInsn(Integer.MIN_VALUE).getOpcode());
		assertEquals(LDC, longToInsn(Integer.MAX_VALUE).getOpcode());
	}

	@Test
	void testGetReturnOpcode() {
		assertEquals(RETURN, getReturnOpcode(VOID_TYPE));
		assertEquals(IRETURN, getReturnOpcode(BOOLEAN_TYPE));
		assertEquals(IRETURN, getReturnOpcode(CHAR_TYPE));
		assertEquals(IRETURN, getReturnOpcode(BYTE_TYPE));
		assertEquals(IRETURN, getReturnOpcode(SHORT_TYPE));
		assertEquals(IRETURN, getReturnOpcode(INT_TYPE));
		assertEquals(FRETURN, getReturnOpcode(FLOAT_TYPE));
		assertEquals(LRETURN, getReturnOpcode(LONG_TYPE));
		assertEquals(DRETURN, getReturnOpcode(DOUBLE_TYPE));
		assertEquals(ARETURN, getReturnOpcode(Types.OBJECT_TYPE));
		assertEquals(ARETURN, getReturnOpcode(Types.STRING_TYPE));
		assertEquals(ARETURN, getReturnOpcode(Types.ARRAY_1D_BYTE));
	}

	@Test
	void testIsReturn() {
		assertTrue(isReturn(new InsnNode(RETURN)));
		assertTrue(isReturn(new InsnNode(IRETURN)));
		assertTrue(isReturn(new InsnNode(LRETURN)));
		assertTrue(isReturn(new InsnNode(FRETURN)));
		assertTrue(isReturn(new InsnNode(DRETURN)));
		assertTrue(isReturn(new InsnNode(ARETURN)));

		assertFalse(isReturn(new InsnNode(NOP)));
		assertFalse(isReturn(new VarInsnNode(ILOAD, 0)));
		assertFalse(isReturn(new LdcInsnNode("return")));
	}

	@Test
	void testIsFlowControl() {
		assertTrue(isFlowControl(new InsnNode(RET))); // Paired to JSR
		assertTrue(isFlowControl(new InsnNode(ATHROW)));
		assertTrue(isFlowControl(new JumpInsnNode(IFEQ, new LabelNode())));
		assertTrue(isFlowControl(new TableSwitchInsnNode(0, 1, new LabelNode(), new LabelNode())));
		assertTrue(isFlowControl(new LookupSwitchInsnNode(new LabelNode(), new int[]{0}, new LabelNode[]{new LabelNode()})));

		assertFalse(isFlowControl(new InsnNode(RETURN)));
		assertFalse(isFlowControl(new InsnNode(NOP)));
		assertFalse(isFlowControl(new VarInsnNode(ILOAD, 0)));
	}

	@Test
	void testIsTerminalOrAlwaysTakeFlowControl() {
		// Throw --> terminal
		assertTrue(isTerminalOrAlwaysTakeFlowControl(ATHROW));

		// Return --> terminal
		assertTrue(isTerminalOrAlwaysTakeFlowControl(RETURN));
		assertTrue(isTerminalOrAlwaysTakeFlowControl(IRETURN));
		assertTrue(isTerminalOrAlwaysTakeFlowControl(FRETURN));
		assertTrue(isTerminalOrAlwaysTakeFlowControl(DRETURN));
		assertTrue(isTerminalOrAlwaysTakeFlowControl(LRETURN));
		assertTrue(isTerminalOrAlwaysTakeFlowControl(ARETURN));

		// Goto, Jsr, Switch --> not conditional
		assertTrue(isTerminalOrAlwaysTakeFlowControl(JSR));
		assertTrue(isTerminalOrAlwaysTakeFlowControl(GOTO));
		assertTrue(isTerminalOrAlwaysTakeFlowControl(LOOKUPSWITCH));
		assertTrue(isTerminalOrAlwaysTakeFlowControl(TABLESWITCH));

		// Conditional jumps
		assertFalse(isTerminalOrAlwaysTakeFlowControl(IFEQ));
		assertFalse(isTerminalOrAlwaysTakeFlowControl(IFNULL));
		assertFalse(isTerminalOrAlwaysTakeFlowControl(IF_ICMPLT));

		// Other instructions
		assertFalse(isTerminalOrAlwaysTakeFlowControl(NOP));
		assertFalse(isTerminalOrAlwaysTakeFlowControl(IDIV));
	}

	@Test
	void testIsSwitchEffectiveGoto() {
		// When all labels are the same, it is basically a goto.
		LabelNode defaultLabel = new LabelNode();
		assertTrue(isSwitchEffectiveGoto(new TableSwitchInsnNode(0, 1, defaultLabel, defaultLabel)));
		assertTrue(isSwitchEffectiveGoto(new LookupSwitchInsnNode(defaultLabel, new int[]{0}, new LabelNode[]{defaultLabel})));

		// When any label is different, its no longer effectively goto.
		assertFalse(isSwitchEffectiveGoto(new TableSwitchInsnNode(0, 1, defaultLabel, new LabelNode())));
		assertFalse(isSwitchEffectiveGoto(new LookupSwitchInsnNode(defaultLabel, new int[]{0}, new LabelNode[]{new LabelNode()})));
	}

	@Test
	void testIsMetaData() {
		// ASM uses '-1' to indicate metadata instructions
		assertTrue(isMetaData(new InsnNode(-1)));
		assertTrue(isMetaData(new FrameNode(0, 0, new Object[0], 0, new Object[0])));
		assertTrue(isMetaData(new LabelNode()));
		assertTrue(isMetaData(new LineNumberNode(24, new LabelNode())));

		assertFalse(isMetaData(new InsnNode(NOP)));
		assertFalse(isMetaData(new IntInsnNode(BIPUSH, 100)));
	}

	@Test
	void testGetNextInsn() {
		InsnList list = new InsnList();
		list.add(new InsnNode(NOP));
		list.add(new LabelNode());
		list.add(new LabelNode());
		list.add(new LabelNode());
		list.add(new LabelNode());
		list.add(new InsnNode(NOP));

		// The "next" instruction skips meta-data values like labels.
		assertSame(list.getLast(), getNextInsn(list.getFirst()));
	}

	@Test
	void testGetNextFollowGoto() {
		LabelNode gotoTarget = new LabelNode();
		InsnList list = new InsnList();
		list.add(new InsnNode(NOP));
		list.add(new JumpInsnNode(GOTO, gotoTarget));
		list.add(gotoTarget);
		list.add(new InsnNode(RETURN));

		// The "next" instruction skips meta-data values like labels and follows goto instructions instead
		// of yielding them.
		assertSame(list.getLast(), getNextFollowGoto(list.getFirst()));

		// Otherwise it is just a normal next (skipping metadata)
		list = new InsnList();
		list.add(new InsnNode(NOP));
		list.add(new LabelNode());
		list.add(new LabelNode());
		list.add(new LabelNode());
		list.add(new LabelNode());
		list.add(new InsnNode(RETURN));
		assertSame(list.getLast(), getNextFollowGoto(list.getFirst()));
	}

	@Test
	void testGetPreviousInsn() {
		InsnList list = new InsnList();
		list.add(new InsnNode(NOP));
		list.add(new LabelNode());
		list.add(new LabelNode());
		list.add(new LabelNode());
		list.add(new LabelNode());
		list.add(new InsnNode(RETURN));

		// The "previous" instruction skips meta-data values like labels.
		assertSame(list.getFirst(), getPreviousInsn(list.getLast()));

		// Standard backwards for no meta-data.
		list = new InsnList();
		list.add(new InsnNode(NOP));
		list.add(new InsnNode(ICONST_0));
		list.add(new InsnNode(POP));
		list.add(new InsnNode(RETURN));
		assertEquals(POP, Objects.requireNonNull(getPreviousInsn(list.getLast())).getOpcode());
	}

	@Test
	void testHasHandlerFlowIntoBlock() {
		// try { return a / b; } catch (...) { return 0; }
		LabelNode tryStart = new LabelNode();
		LabelNode tryEnd = new LabelNode();
		LabelNode tryHandler = new LabelNode();
		MethodNode method = new MethodNode(ACC_STATIC, "div", "(II)I", null, null);
		method.tryCatchBlocks.add(new TryCatchBlockNode(tryStart, tryEnd, tryHandler, null));
		method.instructions.add(tryStart);
		method.instructions.add(new VarInsnNode(ILOAD, 0));
		method.instructions.add(new VarInsnNode(ILOAD, 1));
		method.instructions.add(new InsnNode(IDIV));
		method.instructions.add(new InsnNode(IRETURN));
		method.instructions.add(tryEnd);
		method.instructions.add(tryHandler);
		method.instructions.add(new InsnNode(ICONST_0));
		method.instructions.add(new InsnNode(IRETURN));

		// Get the last 3 instructions starting with the handler label.
		List<AbstractInsnNode> block = new ArrayList<>();
		ListIterator<AbstractInsnNode> it = method.instructions.iterator(method.instructions.indexOf(tryHandler));
		while (it.hasNext())
			block.add(it.next());

		// If we skip the first insn, then we don't see that the catch handler is in the block.
		assertFalse(hasHandlerFlowIntoBlock(method, block, false));

		// If we include the first insn, then we do see the catch handler is the first insn in the block.
		assertTrue(hasHandlerFlowIntoBlock(method, block, true));
	}

	@Test
	void testHasInboundFlowReferencesFromJump() {
		LabelNode gotoDest = new LabelNode();
		MethodNode method = new MethodNode(ACC_STATIC, "div", "(II)V", null, null);
		method.instructions.add(new VarInsnNode(ILOAD, 0));
		method.instructions.add(new JumpInsnNode(IFEQ, gotoDest));
		method.instructions.add(new InsnNode(RETURN));
		method.instructions.add(gotoDest);
		method.instructions.add(new InsnNode(RETURN));

		// If the block is the whole method then it cannot possibly have inbound references
		List<AbstractInsnNode> block = new ArrayList<>();
		ListIterator<AbstractInsnNode> it = method.instructions.iterator(0);
		while (it.hasNext())
			block.add(it.next());
		assertFalse(hasInboundFlowReferences(method, block));

		// If we have a label used by the ifeq, it will have an inbound reference
		block.clear();
		it = method.instructions.iterator(method.instructions.indexOf(gotoDest));
		while (it.hasNext())
			block.add(it.next());
		assertTrue(hasInboundFlowReferences(method, block));

		// And if we exclude that label, back to being false.
		block.clear();
		it = method.instructions.iterator(method.instructions.indexOf(gotoDest) + 1);
		while (it.hasNext())
			block.add(it.next());
		assertFalse(hasInboundFlowReferences(method, block));
	}

	@Test
	void testHasInboundFlowReferencesFromTableSwitch() {
		LabelNode labelA = new LabelNode();
		LabelNode labelB = new LabelNode();
		LabelNode labelC = new LabelNode();
		MethodNode method = new MethodNode(ACC_STATIC, "div", "(I)V", null, null);
		method.instructions.add(new VarInsnNode(ILOAD, 0));
		method.instructions.add(new TableSwitchInsnNode(0, 1, labelB, labelA));
		method.instructions.add(labelA);
		method.instructions.add(new InsnNode(RETURN));
		method.instructions.add(labelB);
		method.instructions.add(new InsnNode(RETURN));
		method.instructions.add(labelC); // Dead code
		method.instructions.add(new InsnNode(RETURN));

		// Check the case
		List<AbstractInsnNode> block = new ArrayList<>();
		ListIterator<AbstractInsnNode> it = method.instructions.iterator(method.instructions.indexOf(labelA));
		while (it.hasNext() && block.size() < 2)
			block.add(it.next());
		assertTrue(hasInboundFlowReferences(method, block));

		// Check the default
		block.clear();
		it = method.instructions.iterator(method.instructions.indexOf(labelB));
		while (it.hasNext() && block.size() < 2)
			block.add(it.next());
		assertTrue(hasInboundFlowReferences(method, block));

		// Dead code --> no inbound flow
		block.clear();
		it = method.instructions.iterator(method.instructions.indexOf(labelC));
		while (it.hasNext())
			block.add(it.next());
		assertFalse(hasInboundFlowReferences(method, block));
	}

	@Test
	void testHasInboundFlowReferencesFromLookupSwitch() {
		LabelNode labelA = new LabelNode();
		LabelNode labelB = new LabelNode();
		LabelNode labelC = new LabelNode();
		MethodNode method = new MethodNode(ACC_STATIC, "div", "(I)V", null, null);
		method.instructions.add(new VarInsnNode(ILOAD, 0));
		method.instructions.add(new LookupSwitchInsnNode(labelB, new int[]{0}, new LabelNode[]{labelA}));
		method.instructions.add(labelA);
		method.instructions.add(new InsnNode(RETURN));
		method.instructions.add(labelB);
		method.instructions.add(new InsnNode(RETURN));
		method.instructions.add(labelC); // Dead code
		method.instructions.add(new InsnNode(RETURN));

		// Check the case
		List<AbstractInsnNode> block = new ArrayList<>();
		ListIterator<AbstractInsnNode> it = method.instructions.iterator(method.instructions.indexOf(labelA));
		while (it.hasNext() && block.size() < 2)
			block.add(it.next());
		assertTrue(hasInboundFlowReferences(method, block));

		// Check the default
		block.clear();
		it = method.instructions.iterator(method.instructions.indexOf(labelB));
		while (it.hasNext() && block.size() < 2)
			block.add(it.next());
		assertTrue(hasInboundFlowReferences(method, block));

		// Dead code --> no inbound flow
		block.clear();
		it = method.instructions.iterator(method.instructions.indexOf(labelC));
		while (it.hasNext())
			block.add(it.next());
		assertFalse(hasInboundFlowReferences(method, block));
	}

	@Test
	void testGetSizeConsumed() {
		// MULTIANEWARRAY --> pop a value off the stack for the given array dimensions
		assertEquals(1, getSizeConsumed(new MultiANewArrayInsnNode("[LExample;", 1)));
		assertEquals(2, getSizeConsumed(new MultiANewArrayInsnNode("[[LExample;", 2)));
		assertEquals(1, getSizeConsumed(new MultiANewArrayInsnNode("[[[LExample;", 1)));

		// INVOKE-X --> pop calling context (unless static invoke) and parameter types
		assertEquals(1, getSizeConsumed(new MethodInsnNode(INVOKEVIRTUAL, "Owner", "methodName", "()V")));
		assertEquals(1, getSizeConsumed(new MethodInsnNode(INVOKESPECIAL, "Owner", "methodName", "()V")));
		assertEquals(1, getSizeConsumed(new MethodInsnNode(INVOKEINTERFACE, "Owner", "methodName", "()V")));
		assertEquals(0, getSizeConsumed(new MethodInsnNode(INVOKESTATIC, "Owner", "methodName", "()V")));
		assertEquals(1, getSizeConsumed(new MethodInsnNode(INVOKESTATIC, "Owner", "methodName", "(I)V")));
		assertEquals(1, getSizeConsumed(new MethodInsnNode(INVOKESTATIC, "Owner", "methodName", "(LExample;)V")));
		assertEquals(1, getSizeConsumed(new MethodInsnNode(INVOKESTATIC, "Owner", "methodName", "([LExample;)V")));
		assertEquals(2, getSizeConsumed(new MethodInsnNode(INVOKESTATIC, "Owner", "methodName", "(J)V")));
		assertEquals(2, getSizeConsumed(new MethodInsnNode(INVOKESTATIC, "Owner", "methodName", "(FF)V")));
		assertEquals(5, getSizeConsumed(new MethodInsnNode(INVOKESTATIC, "Owner", "methodName", "(JFJ)V")));

		// INVOKE-DYNAMIC -> Pop argument types from method type (derived from descriptor)
		Handle bogusHandle = new Handle(H_INVOKESTATIC, "Owner", "name", "()V", false);
		assertEquals(0, getSizeConsumed(new InvokeDynamicInsnNode("name", "()V", bogusHandle)));
		assertEquals(1, getSizeConsumed(new InvokeDynamicInsnNode("name", "(I)V", bogusHandle)));
		assertEquals(2, getSizeConsumed(new InvokeDynamicInsnNode("name", "(II)V", bogusHandle)));
		assertEquals(2, getSizeConsumed(new InvokeDynamicInsnNode("name", "(J)V", bogusHandle)));
		assertEquals(3, getSizeConsumed(new InvokeDynamicInsnNode("name", "(JI)V", bogusHandle)));

		// GET-X -> pop field context (unless static)
		assertEquals(0, getSizeConsumed(new FieldInsnNode(GETSTATIC, "Owner", "name", "I")));
		assertEquals(1, getSizeConsumed(new FieldInsnNode(GETFIELD, "Owner", "name", "I")));

		// PUT-X -> pop field context (unless static) and field type
		assertEquals(1, getSizeConsumed(new FieldInsnNode(PUTSTATIC, "Owner", "name", "I")));
		assertEquals(2, getSizeConsumed(new FieldInsnNode(PUTFIELD, "Owner", "name", "I")));
		assertEquals(2, getSizeConsumed(new FieldInsnNode(PUTSTATIC, "Owner", "name", "J")));
		assertEquals(3, getSizeConsumed(new FieldInsnNode(PUTFIELD, "Owner", "name", "J")));

		// metadata --> nothing
		assertEquals(0, getSizeConsumed(new LabelNode()));
		assertEquals(0, getSizeConsumed(new LineNumberNode(25, new LabelNode())));
		assertEquals(0, getSizeConsumed(new FrameNode(0, 0, new Object[0], 0, new Object[0])));

		// TODO: The rest are specific per-op and not by instruction 'type'
	}

	@Test
	void testGetSizeProduced() {
		// INVOKE-X --> return type produced regardless of opcode
		assertEquals(0, getSizeProduced(new MethodInsnNode(INVOKEVIRTUAL, "Owner", "methodName", "()V")));
		assertEquals(0, getSizeProduced(new MethodInsnNode(INVOKESPECIAL, "Owner", "methodName", "()V")));
		assertEquals(0, getSizeProduced(new MethodInsnNode(INVOKEINTERFACE, "Owner", "methodName", "()V")));
		assertEquals(0, getSizeProduced(new MethodInsnNode(INVOKESTATIC, "Owner", "methodName", "()V")));
		assertEquals(1, getSizeProduced(new MethodInsnNode(INVOKESTATIC, "Owner", "methodName", "()I")));
		assertEquals(2, getSizeProduced(new MethodInsnNode(INVOKESTATIC, "Owner", "methodName", "()J")));
		assertEquals(1, getSizeProduced(new MethodInsnNode(INVOKESTATIC, "Owner", "methodName", "()[J")));

		// INVOKE-DYNAMIC -> return type produced (from descriptor)
		Handle bogusHandle = new Handle(H_INVOKESTATIC, "Owner", "name", "()V", false);
		assertEquals(0, getSizeProduced(new InvokeDynamicInsnNode("name", "()V", bogusHandle)));
		assertEquals(0, getSizeProduced(new InvokeDynamicInsnNode("name", "(I)V", bogusHandle)));
		assertEquals(1, getSizeProduced(new InvokeDynamicInsnNode("name", "()I", bogusHandle)));
		assertEquals(2, getSizeProduced(new InvokeDynamicInsnNode("name", "()J", bogusHandle)));
		assertEquals(1, getSizeProduced(new InvokeDynamicInsnNode("name", "()[J", bogusHandle)));

		// GET-X -> field type produced (regardless of static)
		assertEquals(1, getSizeProduced(new FieldInsnNode(GETFIELD, "Owner", "name", "I")));
		assertEquals(1, getSizeProduced(new FieldInsnNode(GETSTATIC, "Owner", "name", "I")));
		assertEquals(2, getSizeProduced(new FieldInsnNode(GETFIELD, "Owner", "name", "J")));
		assertEquals(1, getSizeProduced(new FieldInsnNode(GETFIELD, "Owner", "name", "[J")));

		// PUT-X -> nothing produced
		assertEquals(0, getSizeProduced(new FieldInsnNode(PUTFIELD, "Owner", "name", "I")));
		assertEquals(0, getSizeProduced(new FieldInsnNode(PUTSTATIC, "Owner", "name", "I")));
		assertEquals(0, getSizeProduced(new FieldInsnNode(PUTFIELD, "Owner", "name", "J")));
		assertEquals(0, getSizeProduced(new FieldInsnNode(PUTFIELD, "Owner", "name", "[J")));

		// LDC --> depends on value
		assertEquals(1, getSizeProduced(new LdcInsnNode(0)));
		assertEquals(1, getSizeProduced(new LdcInsnNode("a")));
		assertEquals(1, getSizeProduced(new LdcInsnNode(1.5F)));
		assertEquals(2, getSizeProduced(new LdcInsnNode(Double.MAX_VALUE)));
		assertEquals(2, getSizeProduced(new LdcInsnNode(Long.MAX_VALUE)));

		// Jump --> Only if JSR
		assertEquals(1, getSizeProduced(new JumpInsnNode(JSR, new LabelNode())));
		assertEquals(0, getSizeProduced(new JumpInsnNode(IFEQ, new LabelNode())));
		assertEquals(0, getSizeProduced(new JumpInsnNode(IF_ACMPNE, new LabelNode())));

		// metadata --> nothing
		assertEquals(0, getSizeProduced(new LabelNode()));
		assertEquals(0, getSizeProduced(new LineNumberNode(25, new LabelNode())));
		assertEquals(0, getSizeProduced(new FrameNode(0, 0, new Object[0], 0, new Object[0])));

		// TODO: The rest are specific per-op and not by instruction 'type'
	}
}