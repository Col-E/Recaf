package me.coley.recaf.util;

/**
 * ASM compacts some opcodes into abstract forms, these constants outline each opcode not directly exposed by ASM.
 *
 * @author Matt Coley
 * @see org.objectweb.asm.Opcodes
 */
public interface NonAbstractOpcodes {
	int ALOAD_0 = 42;
	int ALOAD_1 = 43;
	int ALOAD_2 = 44;
	int ALOAD_3 = 45;
	int ASTORE_0 = 75;
	int ASTORE_1 = 76;
	int ASTORE_2 = 77;
	int ASTORE_3 = 78;
	int DLOAD_0 = 38;
	int DLOAD_1 = 39;
	int DLOAD_2 = 40;
	int DLOAD_3 = 41;
	int DSTORE_0 = 71;
	int DSTORE_1 = 72;
	int DSTORE_2 = 73;
	int DSTORE_3 = 74;
	int FLOAD_0 = 34;
	int FLOAD_1 = 35;
	int FLOAD_2 = 36;
	int FLOAD_3 = 37;
	int FSTORE_0 = 67;
	int FSTORE_1 = 68;
	int FSTORE_2 = 69;
	int FSTORE_3 = 70;
	int GOTO_W = 200;
	int ILOAD_0 = 26;
	int ILOAD_1 = 27;
	int ILOAD_2 = 28;
	int ILOAD_3 = 29;
	int ISTORE_0 = 59;
	int ISTORE_1 = 60;
	int ISTORE_2 = 61;
	int ISTORE_3 = 62;
	int LDC2_W = 20;
	int LDC_W = 19;
	int LLOAD_0 = 30;
	int LLOAD_1 = 31;
	int LLOAD_2 = 32;
	int LLOAD_3 = 33;
	int LSTORE_0 = 63;
	int LSTORE_1 = 64;
	int LSTORE_2 = 65;
	int LSTORE_3 = 66;
}
